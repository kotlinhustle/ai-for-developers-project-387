# AGENTS.md

Веб-приложение «Календарь звонков»: backend (Kotlin + Spring Boot) и frontend
(React + TypeScript + Vite). Владелец создаёт типы встреч и видит записи; гость
бронирует свободный слот. Демо предыдущего проекта (репозиторий
`ai-for-developers-project-386`): https://calendar-y6sf.onrender.com — Render для
этого репозитория (`ai-for-developers-project-387`) пока не настроен.

## Структура

```
backend/                       Kotlin + Spring Boot 4.1.1, JDK 21, Gradle Wrapper 9.7.1
  build.gradle.kts             зависимости, ktlint, версия (x-release-please-version)
  src/main/kotlin/             код приложения
  src/main/resources/          application.properties
  src/test/kotlin/             тесты, включая HealthSmokeTest (random port, /actuator/health)
frontend/                      React 19 + TypeScript 6.0 + Vite 8.3 + react-router 8
  vite.config.ts               proxy /api -> http://localhost:8080, конфиг Vitest
  eslint.config.js             ESLint flat config
  src/pages/                   HomePage, BookingPage, OwnerPage
  src/App.test.tsx             тесты (Vitest + Testing Library)
spec/                          TypeSpec-источник контракта (main.tsp, tspconfig.yaml)
  npm run generate             TypeSpec -> OpenAPI -> клиентский SDK + серверные артефакты
e2e/                           браузерные тесты Playwright (нужны backend + Postgres или H2 + frontend)
  npm test                     основной сценарий + конфликт бронирования
Dockerfile                     образ: контракт + фронтенд + бэкенд, один порт (PORT)
docker-compose.yml             Postgres (+ app) для локального запуска
opencode.json                  проектная MCP-конфигурация (Render, ключ из env)
.github/workflows/
  ci.yml                       push + pull_request: Backend, Frontend, E2E, Docker
  opencode.yml                 интерактивный агент: /oc или /opencode в комментарии
  opencode-triage.yml          issue opened: разбор жалобы агентом (только комментарий)
  opencode-review.yml          pull_request: авторевью PR агентом (только комментарий)
  opencode-audit.yml           schedule + workflow_dispatch: аудит документации агентом
  release-please.yml           push в main: версия и CHANGELOG
  pr-title.yml                 проверка заголовка PR (Conventional Commits)
  hexlet-check.yml             НЕ ТРОГАТЬ (генерируется Хекслетом)
release-please-config.json     настройки релизов
.release-please-manifest.json  текущая версия
version.txt                    текущая версия (simple release type)
```

Версия `0.1.0` синхронизирована между `version.txt`, `.release-please-manifest.json`,
`backend/build.gradle.kts` (строка с `// x-release-please-version`) и
`frontend/package.json`. При релизе release-please обновляет все четыре файла.

## Проверенные команды

Раннер GitHub Actions — Linux (`ubuntu-latest`), поэтому для CI каноничны
команды из блоков «Linux / macOS».

Бэкенд (Windows PowerShell):

```powershell
cd backend
.\gradlew.bat ktlintCheck test build
```

Бэкенд (Linux / macOS):

```bash
cd backend
chmod +x gradlew          # на свежем Linux-чекауте и на раннере GitHub
./gradlew ktlintCheck test build
```

- На Linux и на раннере GitHub у `gradlew` нет бита на исполнение: без
  `chmod +x gradlew` команда `./gradlew` завершится с `Permission denied`
  (CI делает `chmod +x ./gradlew` явно).
- Автоформат Kotlin: `.\gradlew.bat ktlintFormat` (или `./gradlew ktlintFormat`).
- Запуск: `.\gradlew.bat bootRun` (или `./gradlew bootRun`), health: `http://localhost:8080/actuator/health`.
- Без `DB_URL` бэкенд поднимается на in-memory H2 (образ стартует без внешней БД); с `DB_URL`/`DB_USER`/`DB_PASSWORD` — Postgres (см. `docker-compose.yml`). Тесты используют встроенный Postgres (embedded) и внешней БД не требуют.

Фронтенд:

```bash
cd frontend
npm ci            # или npm install при первом запуске
npm run lint
npm run typecheck
npm test
npm run build
```

- Запуск dev-сервера: `npm run dev` (http://localhost:5173).
- Ограничение зависимостей: `react-router` — единственная новая runtime-зависимость; тестовые пакеты (Vitest, Testing Library, jsdom) — только `devDependencies`.

Генерация контракта (Design First), из `spec/`:

```bash
cd spec
npm ci            # или npm install при первом запуске
npm run generate
```

- Одна команда: TypeSpec → OpenAPI → клиентский SDK (TS) и серверные артефакты (Kotlin/Spring).
- Сгенерированные каталоги (`spec/tsp-output/`, `frontend/src/generated/`, `backend/build/generated/`) руками не редактируются и не коммитятся; CI регенерирует их перед сборкой.
- Backend компилирует сгенерированные Kotlin-интерфейсы (`build/generated/openapi/...`), ktlint их исключает.

Все команды из этого файла проверены локально.

## Формат коммитов

Используются [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short description>
```

- `type`: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`.
- `scope` (опционально): `backend`, `frontend`, `ci`, `release` и т.п.
- Описание — в повелительном наклонении, на английском.

Примеры:

```
feat(backend): add health smoke test
fix(frontend): handle backend unavailable state
ci: run frontend typecheck on pull requests
```

release-please формирует версии и CHANGELOG именно из этих сообщений,
а `.github/workflows/pr-title.yml` проверяет заголовки PR.

## Правила для агентов

- Не удалять и не редактировать `.github/workflows/hexlet-check.yml`.
- Не коммитить `node_modules/`, `build/`, `dist/`.
- После изменений в backend запускать `ktlintCheck` (или `ktlintFormat`), после изменений во frontend — `lint` и `typecheck`.
- В описании PR закрывающую ссылку (`Closes #N`) размещать обычным текстом,
  вне блока кода, и сохранять при любом редактировании описания — иначе issue
  не закроется автоматически при мерже.

## Agent skills

### Issue tracker

Issues and specs live as GitHub issues, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default canonical vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout: `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
