# WorkflowRAG

Java 21 / Spring Boot 4.1.1: версионирование документов, управляемая индексация и поиск по локальной базе знаний. PostgreSQL 17 + pgvector хранят документы, стадии обработки и поисковые индексы. Embeddings вычисляет Ollama/BGE-M3.

## Быстрый запуск

Нужны JDK 21, Docker и Ollama. Команды выполняются из корня репозитория.

```powershell
docker compose -f infra/docker-compose.yml up -d
ollama pull bge-m3
./mvnw.cmd verify
java -jar WorkflowRAG-src/target/WorkflowRAG-src-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

На Linux/macOS вместо `./mvnw.cmd` используйте `./mvnw`. Подойдёт и установленный Maven: `mvn verify`. Ollama должен обслуживать `http://localhost:11434` (при необходимости запустите `ollama serve` отдельно).

Приложение работает на порту 8080. Профиль `dev` использует PostgreSQL из Compose на порту 55432 и источник `demo`, указывающий на папку `knowledge`. В ней лежит пример `getting-started.md`. Собственный `application-local.yml` остаётся локальной конфигурацией; для приведённых команд он не нужен.

Настройки можно задать переменными окружения: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `OLLAMA_BASE_URL`, `EMBEDDING_MODEL`, `KNOWLEDGE_ROOT`. Текущая размерность векторной схемы — 1024; замена модели на другую размерность требует отдельной миграции.

## Загрузка, поиск и контекст

Пример для PowerShell:

```powershell
$body = @{ sourceId = 'demo'; externalDocumentId = 'getting-started.md' } | ConvertTo-Json
$ingestion = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/ingestions/source -ContentType application/json -Body $body
$ingestion

$query = @{ query = 'What happens when an update fails?'; sourceId = 'demo'; profile = 'HYBRID'; limit = 3 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/retrieval/search -ContentType application/json -Body $query

$context = @{ query = 'document lifecycle'; sourceId = 'demo'; profile = 'HYBRID'; limit = 3; expansionStrategy = 'SECTION_IF_SMALL' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/retrieval/context -ContentType application/json -Body $context

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/jobs/$($ingestion.jobId)"
```

Загрузка синхронная: сначала коммитятся версия и Job, затем выполняется `parse → chunk → embed → index → activate`. Успешный ответ содержит `jobId` и `status: COMPLETED`. Повтор той же загрузки возвращает `UNCHANGED` и `jobId: null`, не создавая новую версию. Время HTTP-запроса включает вычисление embeddings.

Поддерживаемые парсером форматы сейчас — TXT и Markdown. Адаптер умеет распознавать дополнительные расширения, но их парсеры пока не реализованы. Путь файла должен быть внутри корня источника. Inline-загрузка, парсинг исходников, UI, MCP и генерация ответов остаются дальнейшими этапами.

## Ошибки и повтор

При сбое Job и строящаяся версия получают FAILED; предыдущая ACTIVE продолжает обслуживать поиск. Ошибка `WORKFLOW_FAILED` содержит ID задачи. После устранения причины её можно продолжить:

```powershell
$jobId = 'UUID-неудачной-задачи'
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/jobs/$jobId/retry"
```

Завершённые стадии повторно не выполняются. Старую неудачную задачу нельзя продолжить, если уже создана более новая версия документа: вернётся 409. При старте приложения прерванные RUNNING/PENDING задачи переводятся в FAILED и доступны для повтора. Автоматического фонового повтора нет.

Запускайте **один экземпляр приложения на одну базу**: текущий механизм восстановления не использует распределённые leases. Несколько параллельных запросов к одному экземпляру допустимы, но одновременно строить две версии одного документа нельзя.

## Поиск и модели

- `SIMPLE`: semantic search, только ACTIVE-версии и точная revision embedding-модели.
- `HYBRID`: semantic + PostgreSQL FTS + RRF. Это не BM25.
- `ADVANCED`: hybrid и reranker. Если reranker выключен, API возвращает 503 `RERANKING_UNAVAILABLE`; используйте SIMPLE/HYBRID.

Расширение контекста: `CHUNK_ONLY`, `SECTION_IF_SMALL`, `NEIGHBOR_CHUNKS`. Маршрутизация источников сопоставляет токены запроса с описаниями; явный `sourceId` задаёт источник напрямую.

ONNX-reranker остаётся опциональным. Для включения задайте `RERANKING_ENABLED=true`, `RERANKING_MODEL_PATH`, `RERANKING_TOKENIZER_PATH` и при необходимости `RERANKING_REVISION`. Совместимые файлы модели и токенизатора должны существовать. Реальные ONNX-файлы не входят в репозиторий; проверка с ними не является частью обычных тестов.

Не заменяйте содержимое тега Ollama во время работы приложения: revision кешируется на время процесса. После обновления модели перезапустите приложение и явно переиндексируйте документы.

## Обновление существующей базы

Liquibase сохраняет первоначальные миграции 005/006, принимает также контрольные суммы двух известных ранее отредактированных вариантов и применяет миграцию 007. Она добавляет/заполняет FTS-индекс существующих chunks, не удаляя документы. Не требуется очищать checksums или удалять Docker volume. Неизвестный вариант старой миграции требует отдельной проверки.

У старых embeddings revision могла быть пустой. Она намеренно не заменяется текущим digest: происхождение вектора нельзя установить таким способом. Для них, а также после смены модели, запросите новую обработку того же файла:

```powershell
$reindex = @{
  sourceId = 'demo'
  externalDocumentId = 'getting-started.md'
  parameters = @{ forceReindex = 'true' }
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/ingestions/source -ContentType application/json -Body $reindex
```

Создаётся новая версия даже при неизменном тексте. Прежняя версия остаётся активной до завершения; совместимые вычисления по fingerprints можно переиспользовать. Для миграции старого источника используйте его настоящий `sourceId` и исходный путь файла, а не `demo`.

## Проверки

`mvn verify` запускает unit-тесты и интеграционные проверки с отдельными Testcontainers PostgreSQL/pgvector, затем собирает запускаемый JAR. Docker должен работать. Пользовательская база и Ollama для автоматических тестов не используются: вместо внешней модели тесты подставляют детерминированный provider, сохраняя настоящие HTTP, workflow, SQL и миграции.

Проверяются загрузка и поиск, пропуск неизменённых документов, reuse embeddings, ошибка новой версии, retry, защита от устаревшего retry, изоляция revision, восстановление прерванного Job и обновление старых схем.

Разработка этой доработки ведётся в `codex/finch`. Слияние в `master` и push выполняет владелец репозитория.
