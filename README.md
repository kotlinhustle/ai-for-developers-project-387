# Календарь звонков

[![hexlet-check](https://github.com/kotlinhustle/ai-for-developers-project-387/actions/workflows/hexlet-check.yml/badge.svg)](https://github.com/kotlinhustle/ai-for-developers-project-387/actions)

Учебный проект Хекслета: сервис для бронирования календаря.
Программа: https://ru.hexlet.io/programs/ai-for-developers

## Демо

**https://calendar-y6sf.onrender.com** — демо **предыдущего проекта**
(репозиторий `ai-for-developers-project-386`). Это развёрнутое приложение на
бесплатном тарифе Render (сервис засыпает при простое, первый запрос после сна —
с задержкой).

> Render для этого репозитория (`ai-for-developers-project-387`) пока не
> настроен: код перенесён, но сервис ещё не пересобран из нового репозитория.
> Актуальная демо-ссылка появится здесь после настройки деплоя.

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

> Раздел описывает деплой **предыдущего проекта** (репозиторий
> `ai-for-developers-project-386`). Render для этого репозитория пока не
> перенастроен; шаги ниже применимы и к нему после создания сервиса.

Публичная демо-ссылка **предыдущего проекта**: **https://calendar-y6sf.onrender.com**

Развёрнуто на Render: бесплатный Docker web service (образ собирается из
репозитория по корневому `Dockerfile`) и бесплатный PostgreSQL 18 (регион
frankfurt). Приложение слушает `PORT`, подключение к БД — через `DB_URL`,
`DB_USER`, `DB_PASSWORD`.

Оговорки бесплатного тарифа: сервис засыпает при простое (первый запрос после
сна — с задержкой); бесплатный PostgreSQL имеет ограниченный срок жизни.

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

Ожидаемое поведение (вкладка B):

- Появляется «Это время уже заняли. Выберите другой слот».
- Занятый слот исчезает из списка **без перезагрузки страницы**.
- Выбор слота сбрасывается, введённые «Имя» и «Email» сохраняются.
- Можно выбрать другой свободный слот и успешно записаться → «Вы записаны».
- Если обновить список не удалось — «Не удалось обновить свободные слоты» и кнопка «Повторить загрузку».

## CI и релизы

- `.github/workflows/ci.yml` — на `push` и `pull_request` запускает job'ы:
  `Backend` (ktlint, тесты, сборка), `Frontend` (lint, типы, тесты, сборка),
  `E2E` (Playwright против preview) и `Docker` (сборка образа + Playwright против образа).
- `.github/workflows/release-please.yml` — на push в `main` обновляет версию и
  CHANGELOG на основе Conventional Commits.
- `.github/workflows/pr-title.yml` — проверяет, что заголовок PR соответствует
  Conventional Commits.
- `.github/workflows/opencode.yml` — интерактивный агент OpenCode: запускается
  командой `/oc` или `/opencode` в комментарии к issue или pull request
  (комментарии ботов игнорируются).
- `.github/workflows/opencode-triage.yml` — при открытии issue агент OpenCode
  разбирает жалобу по коду и оставляет комментарий (issues от ботов
  пропускаются); код не меняется, PR не создаётся, issue не закрывается.

Версия приложения (`0.1.0`) синхронизирована между `version.txt`,
`.release-please-manifest.json`, `backend/build.gradle.kts` и
`frontend/package.json`. release-please обновляет все эти файлы при релизе.

---

<details>
<summary>Автоматические тесты Хекслета</summary>

Тесты запускаются на каждый коммит. За запуск отвечает файл `.github/workflows/hexlet-check.yml` — не удаляйте и не переименовывайте ни его, ни репозиторий.

</details>

## О Хекслете

[Хекслет](https://ru.hexlet.io/) — школа программирования: авторские программы обучения с практикой, поддержкой наставников и реальными проектами, которые остаются в резюме. Этот репозиторий — один из таких проектов.
