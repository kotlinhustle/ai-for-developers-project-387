# Календарь звонков

[![hexlet-check](https://github.com/kotlinhustle/ai-for-developers-project-387/actions/workflows/hexlet-check.yml/badge.svg)](https://github.com/kotlinhustle/ai-for-developers-project-387/actions)

Учебный проект Хекслета: сервис для бронирования календаря.
Программа: https://ru.hexlet.io/programs/ai-for-developers

## Демо

**https://calendar-387-h7q2.onrender.com** — демо **этого проекта**
(`ai-for-developers-project-387`). Бесплатный Docker web service на Render
(регион frankfurt). Внешняя БД не подключена: приложение работает на встроенной
in-memory H2, поэтому данные не переживают перезапуск/сон (см. «Деплой (Render)»).

Проверено на деплое: `/actuator/health` → `{"status":"UP"}`, отдаются главная,
`/booking` и `/owner`, сценарий бронирования (создание типа встречи → свободные
слоты → запись) проходит через API.

Демо **предыдущего проекта** (репозиторий `ai-for-developers-project-386`):
**https://calendar-y6sf.onrender.com** — отдельный сервис, он не изменялся.
Ссылка обслуживает сборку из 386, а не из этого репозитория.

## Стек

- **backend/** — Kotlin, Spring Boot 4.1.1, JDK 21, Gradle Kotlin DSL + Gradle Wrapper 9.7.1, Spring Data JPA, Flyway, Postgres (H2 без `DB_URL`), Spring Boot Actuator.
- **frontend/** — React 19, TypeScript 6.0, Vite 8.3, react-router 8, ESLint, Vitest + Testing Library.

Зафиксированные версии: Spring Boot `4.1.1`, Kotlin `2.3.21`, Gradle `9.7.1`, React `19.3.0`, Vite `8.3.4`, TypeScript `~6.0.2`, ESLint `10.12.0`, ktlint-gradle `14.2.0`.

Ограничение зависимостей frontend: `react-router` — **единственная новая runtime-зависимость**. Всё тестовое (Vitest, Testing Library, jsdom) — только `devDependencies`.

## Структура

```
.
├── backend/                     # Kotlin + Spring Boot
│   ├── src/main/kotlin/         # код приложения
│   ├── src/main/resources/      # application.properties
│   ├── src/test/kotlin/         # тесты (в т.ч. HealthSmokeTest)
│   └── build.gradle.kts
├── frontend/                    # React + TypeScript + Vite
│   ├── src/
│   │   ├── pages/               # HomePage, BookingPage, OwnerPage
│   │   ├── api.ts               # сконфигурированный клиент из generated/
│   │   └── App.test.tsx         # тесты (Vitest + Testing Library)
│   ├── vite.config.ts           # proxy /api -> backend, конфиг Vitest
│   └── eslint.config.js
├── spec/                        # TypeSpec-источник контракта (Design First)
│   ├── main.tsp
│   └── tspconfig.yaml
├── e2e/                         # браузерные тесты Playwright
├── Dockerfile                   # сборка и запуск приложения одним образом
├── docker-compose.yml           # Postgres (+ приложение) для локального запуска
├── .github/workflows/           # CI (Backend, Frontend, E2E, Docker), release-please, PR-заголовки
├── release-please-config.json   # настройки релизов
├── .release-please-manifest.json
├── version.txt
└── opencode.json                # проектная MCP-конфигурация (Render, ключ из env)
```

## Требования

- JDK 21
- Node.js 24 и npm
- (опционально) IntelliJ IDEA

Gradle устанавливать не нужно — используется wrapper из `backend/`.

## База данных

С заданной переменной `DB_URL` (плюс `DB_USER`, `DB_PASSWORD`) бэкенд работает с
Postgres. **Без `DB_URL`** приложение поднимается на встроенной in-memory H2 — так
образ стартует без внешней БД (например, в автопроверке).

Postgres для локального запуска — через Docker Compose (в корне):

```bash
docker compose up -d
# затем запускать бэкенд с:
#   DB_URL=jdbc:postgresql://localhost:5432/calendar DB_USER=postgres DB_PASSWORD=postgres
```

По умолчанию (без `DB_URL`) H2 держит данные в памяти и не переживает перезапуск;
для постоянных данных используйте Postgres через `DB_URL`.

Тесты бэкенда Postgres не требуют — поднимается встроенный (embedded) Postgres.

## Запуск бэкенда

Windows (PowerShell):

```powershell
cd backend
.\gradlew.bat bootRun
```

Linux / macOS:

```bash
cd backend
./gradlew bootRun
```

Проверка:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## Запуск фронтенда

```bash
cd frontend
npm install
npm run dev
```

Откройте http://localhost:5173 — на главной странице описано, как работает
сервис, а кнопка «Записаться на звонок» ведёт на `/booking`. Запросы к `/api/*`
проксируются на `http://localhost:8080` (см. `frontend/vite.config.ts`).

## Проверки

Бэкенд (линтер, тесты, сборка):

Windows:

```powershell
cd backend
.\gradlew.bat ktlintCheck test build
```

Linux / macOS:

```bash
cd backend
./gradlew ktlintCheck test build
```

Автоформат Kotlin-кода: замените `ktlintCheck` на `ktlintFormat`.

Фронтенд (линтер, типы, сборка):

```bash
cd frontend
npm ci
npm run lint
npm run typecheck
npm test
npm run build
```

## Генерация API

Контракт описан в TypeSpec (`spec/`). Одна команда генерирует OpenAPI, клиентский
SDK (TypeScript) и серверные артефакты (Kotlin/Spring):

```bash
cd spec
npm install        # только при первом запуске
npm run generate
```

Генерируемые каталоги (`spec/tsp-output/`, `frontend/src/generated/`,
`backend/build/generated/`) не редактируются руками и не коммитятся — CI
регенерирует их перед сборкой.

## Docker

Образ собирает контракт (TypeSpec → OpenAPI → клиентский SDK и серверные
артефакты), фронтенд и бэкенд, и запускает приложение одним процессом: бэкенд
раздаёт собранный фронтенд и API на **одном порту**. Порт задаётся переменной
`PORT` (по умолчанию 8080), подключение к Postgres — `DB_URL`, `DB_USER`,
`DB_PASSWORD`. Vite dev-server и его proxy в контейнере не используются.

Сборка образа из корня репозитория:

```bash
docker build -t calendar-app .
```

Полный запуск (Postgres + приложение) через Docker Compose:

```bash
docker compose up -d --build
```

Откройте **http://localhost:9000** — интерфейс и API на одном порту
(`/`, `/booking`, `/owner`, `/meeting-types`, `/bookings`).

Альтернатива — запустить образ вручную в сети Compose (имя сети задано в
`docker-compose.yml`: `name: calendar` → сеть `calendar_default`):

```bash
docker compose up -d postgres
docker run --rm -p 9000:9000 \
  --network calendar_default \
  -e PORT=9000 \
  -e DB_URL=jdbc:postgresql://postgres:5432/calendar \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  calendar-app
```

Без переменных БД образ тоже стартует — приложение поднимается на in-memory H2
(так работает автопроверка): `docker run -p 8080:8080 -e PORT=8080 calendar-app`.

Для локальной разработки бэкенд можно запускать вообще без Postgres — без `DB_URL`
он использует in-memory H2. Postgres при необходимости: `docker compose up -d postgres`,
затем передать `DB_URL`.

## Деплой (Render)

### Проект 387 (этот репозиторий)

Развёрнуто на Render: **бесплатный** Docker web service `calendar-387`
(регион frankfurt, branch `main`, корневой `Dockerfile`).

- **Публичная ссылка:** **https://calendar-387-h7q2.onrender.com**
- **PORT:** `10000` (задан в переменных сервиса; приложение слушает его — в логах
  `Tomcat started on port 10000`).
- **БД:** внешняя БД не подключена — `DB_URL`/`DB_USER`/`DB_PASSWORD` не заданы,
  поэтому приложение работает на встроенной in-memory H2. Это вынужденно: Render
  разрешает **только одну** активную бесплатную БД на workspace, и её занимает
  проект 386; платную БД не создавали.
- **Проверено на деплое:** `/actuator/health` → `{"status":"UP"}`; главная,
  `/booking`, `/owner` отдают SPA; сценарий бронирования (создание типа встречи →
  слоты → запись) проходит через API.
- Оговорки бесплатного тарифа: сервис засыпает при простое, первый запрос после
  сна — с задержкой (до ~1–2 минут).

Сервис проекта **386** (`calendar`) и его БД (`calendar-db`) **не изменялись**.
Когда освободится слот бесплатной БД (у 386 срок ограничен), сюда можно добавить
PostgreSQL и задать `DB_URL`/`DB_USER`/`DB_PASSWORD` — приложение подхватит его
автоматически.

### Проект 386 (предыдущий)

Публичная демо-ссылка **предыдущего проекта**: **https://calendar-y6sf.onrender.com**
— отдельный бесплатный Docker web service и бесплатный PostgreSQL 18 (регион
frankfurt). Приложение слушает `PORT`, подключение к БД — через `DB_URL`,
`DB_USER`, `DB_PASSWORD`.

## Браузерные тесты (Playwright)

Основной сценарий: владелец создаёт тип встречи → гость выбирает тип и свободный
слот → бронирует → видит подтверждение → владелец видит запись. Проверяются также
конфликт брони (`409`) и отсутствие занятого времени среди свободных слотов.

Нужны запущенные бэкенд (с Postgres или H2) и фронтенд (preview-сборка), затем:

```bash
cd e2e
npm install
npx playwright install chromium
E2E_BASE_URL=http://localhost:4173 npm test
```

`E2E_BASE_URL` по умолчанию — `http://localhost:4173` (preview фронтенда).
В CI job `E2E` поднимает Postgres, бэкенд и фронтенд и запускает эти тесты.

## Проверка конфликта бронирования

Воспроизведение конфликта брони в двух вкладках.

Подготовка:

1. Запустить бэкенд на H2: `cd backend && ./gradlew bootRun` (http://localhost:8080).
2. Запустить фронтенд: `cd frontend && npm run dev` (http://localhost:5173).
3. Владелец создаёт тип встречи: http://localhost:5173/owner → «Название», «Длительность, мин» → «Создать тип встречи».

Шаги:

1. Открыть две вкладки гостя: http://localhost:5173/booking (вкладки A и B).
2. В обеих вкладках выбрать один и тот же созданный тип встречи — списки свободных слотов совпадают.
3. Вкладка A: выбрать первый слот, заполнить «Имя» и «Email», нажать «Записаться» → «Вы записаны».
4. Вкладка B (её список ещё показывает занятый слот): выбрать тот же слот, заполнить «Имя» и «Email», нажать «Записаться».
5. Вкладка B после конфликта: выбрать другой свободный слот и нажать «Записаться» — имя и email не вводить повторно.

Ожидаемое поведение (вкладка B):

- Появляется «Это время уже заняли. Выберите другой слот».
- Занятый слот исчезает из списка **без перезагрузки страницы**.
- Выбор слота сбрасывается, введённые «Имя» и «Email» сохраняются.
- Можно выбрать другой свободный слот и успешно записаться → «Вы записаны».
- Если обновить список не удалось — устаревшие слоты скрываются (их нельзя выбрать), показывается «Не удалось обновить свободные слоты» и кнопка «Повторить загрузку».
- После восстановления соединения нажатие «Повторить загрузку» возвращает актуальный список свободных слотов.

## CI и релизы

- `.github/workflows/ci.yml` — на `push` и `pull_request` запускает job'ы:
  `Backend` (ktlint, тесты, сборка), `Frontend` (lint, типы, тесты, сборка),
  `E2E` (Playwright против preview) и `Docker` (сборка образа + Playwright против образа).
- `.github/workflows/release-please.yml` — на push в `main` обновляет версию и
  CHANGELOG на основе Conventional Commits.
- `.github/workflows/pr-title.yml` — проверяет, что заголовок PR соответствует
  Conventional Commits.
- `.github/workflows/opencode*.yml` — агентные workflow OpenCode (интерактивный
  агент, триаж, авторевью, аудит). События, права и расписание — в разделе
  «Агентные workflow OpenCode».

Версия приложения (`0.1.0`) синхронизирована между `version.txt`,
`.release-please-manifest.json`, `backend/build.gradle.kts` и
`frontend/package.json`. release-please обновляет все эти файлы при релизе.

## Агентные workflow OpenCode

Все агентные workflow используют модель `opencode/deepseek-v4.1-flash`, ключ
`OPENCODE_API_KEY` и запускаются на `ubuntu-latest`.

**Репозиторий публичный**, поэтому:

- публичный шаринг сессий отключён (`share: false`) — сессии агента не
  выкладываются на opencode.ai;
- интерактивный агент ограничен пользователями с доступом на запись
  (`author_association` ∈ `OWNER`/`MEMBER`/`COLLABORATOR`), комментарии ботов
  исключены;
- авторевью пропускает draft-PR и события от ботов, но PR, созданные агентом,
  ревьюятся (фильтр — по актору события, а не по автору PR).

| Workflow | События | Модель | Назначение | Права (`permissions`) | Где смотреть запуск и результат |
| --- | --- | --- | --- | --- | --- |
| `opencode.yml` | `issue_comment` (`created`), `pull_request_review_comment` (`created`) | `opencode/deepseek-v4.1-flash` | Интерактивный агент по команде `/oc` или `/opencode` от пользователей с доступом на запись | `contents: read`, `pull-requests: write`, `id-token: write` | Actions → `opencode`; ответ — комментарий к issue/PR |
| `opencode-triage.yml` | `issues` (`opened`) | `opencode/deepseek-v4.1-flash` | Разбор новой жалобы по коду: причина, файлы, план; комментарий к issue | `contents: read`, `issues: write`, `id-token: write` | Actions → `opencode-triage`; результат — комментарий к issue |
| `opencode-review.yml` | `pull_request` (`opened`, `synchronize`, `reopened`, `ready_for_review`) | `opencode/deepseek-v4.1-flash` | Авторевью PR: баги, тесты, AGENTS.md, API-контракт; `use_github_token: true` | `contents: read`, `pull-requests: write`, `issues: write`, `id-token: write` | Actions → `opencode-review`; результат — комментарий к PR |
| `opencode-audit.yml` | `schedule` (`23 22 * * *`), `workflow_dispatch` | `opencode/deepseek-v4.1-flash` | Аудит документации: отчёт и Issue по подтверждённому расхождению | `contents: write`, `pull-requests: write`, `issues: write`, `id-token: write` | Actions → `opencode-audit`; отчёт — в артефакте `docs-audit` |

**Права и почему именно они.**

- `contents: read` — checkout репозитория; больше `contents` не нужно, потому что
  агент не коммитит и не пушит.
- `pull-requests: write` — прочитать PR и публиковать ревью/правки через `gh`
  (с `GH_TOKEN`); одних `read`-прав для публикации недостаточно.
- `issues: write` — публикация комментариев и создание Issue (действие постит
  результат через issues API).
- `id-token: write` — обмен OIDC-токена на токен OpenCode App (для workflow без
  `use_github_token: true`).
- `GH_TOKEN` (`secrets.GITHUB_TOKEN`) — чтобы `gh` был авторизован и агент не
  искал токены на диске (`~/.config/gh`).
- `use_github_token: true` (только авторевью) — действие использует
  `GITHUB_TOKEN` напрямую; в этом режиме `id-token: write` не требуется
  технически, но выдан для соответствия заданию (см. ниже).

**Соответствие заданию.** По требованиям этапа в workflow явно заданы такие права:

- `opencode-review.yml` — дополнительно `id-token: write` (пункт задания
  «добавь `id-token: write` в авторевью»).
- `opencode-audit.yml` — `contents: write`, `pull-requests: write`,
  `issues: write`, `id-token: write`. Аудит по факту не меняет код и не создаёт PR
  (пишет отчёт в игнорируемый `audit-output/` и создаёт Issue), поэтому
  `contents: write` и `pull-requests: write` здесь выданы **для соответствия
  заданию**, а не потому, что нужны агенту.

**Фильтрация ботов.** Во всех агентных workflow есть исключение ботов:
`opencode.yml` — `comment.user.type != 'Bot'` и `!endsWith(actor, '[bot]')`
(плюс ограничение по `author_association`); `opencode-triage.yml` —
`issue.user.type != 'Bot'` и `!endsWith(issue.user.login, '[bot]')`;
`opencode-review.yml` — `!endsWith(actor, '[bot]')`; `opencode-audit.yml` —
`github.event_name == 'schedule' || !endsWith(actor, '[bot]')`, то есть фильтр не
блокирует штатный запуск по расписанию.

**Расписание и таймауты.**

- Аудит: `23 22 * * *` — ежедневно в **22:23 UTC**, то есть **02:23 Asia/Yerevan**
  (следующего дня). GitHub может задержать запуск по расписанию при нагрузке.
- Таймауты: `opencode.yml`, `opencode-triage.yml`, `opencode-review.yml` — 10
  минут; `opencode-audit.yml` — 15 минут.

**Как отключить автоматические запуски.**

- Аудит: убрать/закомментировать блок `schedule` в `opencode-audit.yml` (оставив
  `workflow_dispatch` для ручного запуска) или отключить workflow в
  Actions → `opencode-audit` → «⋯» → Disable workflow.
- Авторевью: отключить workflow `opencode-review` в Actions или убрать типы из
  `pull_request.types`.
- Триаж: отключить workflow `opencode-triage` в Actions.
- Интерактивный агент: отключить workflow `opencode` в Actions — команды `/oc` и
  `/opencode` перестанут вызывать агента.

## Самооценка по истории проекта

Коротко и честно о том, что подтверждено прогонами.

- **Разбор Issues (триаж).** Issue #2 «После неудачной записи календарь предлагает
  занятое время» обработан `opencode-triage` — прогон
  [38611408858](https://github.com/kotlinhustle/ai-for-developers-project-387/actions/runs/38611408858)
  (success), результат — комментарий с разбором.
- **Исправление бага.** По issue #2 создан и смержен PR #3
  [«fix(frontend): refresh slots after booking conflict»](https://github.com/kotlinhustle/ai-for-developers-project-387/pull/3):
  при `409` список слотов перезагружается, устаревшие слоты скрываются, добавлены
  тесты. Issue #2 закрыт как выполненный.
- **Оба канала ревью (интерактивный агент).** Агент отвечал и на обычный
  комментарий к PR (`issue_comment`), и на комментарий к строке кода
  (`pull_request_review_comment`) — прогоны
  [38623117923](https://github.com/kotlinhustle/ai-for-developers-project-387/actions/runs/38623117923)
  и
  [38624067414](https://github.com/kotlinhustle/ai-for-developers-project-387/actions/runs/38624067414).
- **Авторевью.** Workflow `opencode-review` на PR #3 — прогон
  [38625073034](https://github.com/kotlinhustle/ai-for-developers-project-387/actions/runs/38625073034)
  (success), комментарий с замечаниями по коду, тестам, AGENTS.md и контракту.
  По замечаниям в PR #3 добавлены коммиты `e3b0408` и `42621d0`.
- **Аудит документации.** `opencode-audit` запущен **вручную**
  (`workflow_dispatch`) — прогон
  [38630143848](https://github.com/kotlinhustle/ai-for-developers-project-387/actions/runs/38630143848)
  (success); найдены и оформлены Issues #5 и #6. **Cron ещё не срабатывал** —
  проверен только ручной запуск.

Где потребовались итерации:

- Интерактивный прогон завис на запросе `external_directory` и был отменён —
  прогон
  [38618439102](https://github.com/kotlinhustle/ai-for-developers-project-387/actions/runs/38618439102);
  исправлено добавлением `GH_TOKEN` и правил доступа (коммит `e8f094e`).
- Release Please падал с ошибкой «GitHub Actions is not permitted to create or
  approve pull requests» — включён соответствующий параметр репозитория (как в
  проекте 386).
- Issue #2 не закрылся автоматически: `Closes #2` оказалось внутри блока кода в
  описании PR — закрыт вручную, правило добавлено в `AGENTS.md`.
- PR #3 не проходил авторевью, пока его ветка не была обновлена из `main` (merge
  без переписывания истории).

---

<details>
<summary>Автоматические тесты Хекслета</summary>

Тесты запускаются на каждый коммит. За запуск отвечает файл `.github/workflows/hexlet-check.yml` — не удаляйте и не переименовывайте ни его, ни репозиторий.

</details>

## О Хекслете

[Хекслет](https://ru.hexlet.io/) — школа программирования: авторские программы обучения с практикой, поддержкой наставников и реальными проектами, которые остаются в резюме. Этот репозиторий — один из таких проектов.
