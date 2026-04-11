# Remote Files

## Текущее решение

- Удалённый backend для вложений: `S3-compatible object storage`.
- Базовый провайдер по умолчанию: `Yandex Object Storage`.
- Локальная копия файла остаётся в `%APPDATA%\Nocombro\files\...`.
- Метаданные файла синкаются через `YDB`, а сам бинарник живёт в bucket.

## Что уже подготовлено в коде

- Добавлен `RemoteFileStorageGateway`.
- Добавлен реальный `S3RemoteFileStorageGateway` на AWS SDK v2.
- Если конфиг не задан, приложение работает через `NoopRemoteFileStorageGateway`.
- `features/files` теперь:
  - сохраняет локальную копию файла как раньше;
  - при наличии конфига отправляет копию в object storage;
  - хранит в `file` таблице `remoteObjectKey` и `remoteStorageProvider`;
  - использует стабильный `syncId` файла для object key.

## Runtime-настройки

Основной способ настройки на Windows:

`%APPDATA%\Nocombro\s3.properties`

Формат файла:

```properties
bucket=nocombro-files
accessKey=<Access key ID>
secretKey=<Secret access key>
endpoint=https://storage.yandexcloud.net
region=ru-central1
prefix=nocombro
```

Обязательный минимум:

- `bucket`
- `accessKey`
- `secretKey`

Необязательные поля:

- `endpoint`
- `region`
- `prefix`

Значения по умолчанию для необязательных полей:

- endpoint: `https://storage.yandexcloud.net`
- region: `ru-central1`
- prefix: `nocombro`

Для dev-сценариев также остаётся fallback на:

- `NOCOMBRO_REMOTE_FILES_BUCKET` или `nocombro.remoteFiles.bucket`
- `NOCOMBRO_REMOTE_FILES_ACCESS_KEY` или `nocombro.remoteFiles.accessKey`
- `NOCOMBRO_REMOTE_FILES_SECRET_KEY` или `nocombro.remoteFiles.secretKey`
- `NOCOMBRO_REMOTE_FILES_ENDPOINT` или `nocombro.remoteFiles.endpoint`
- `NOCOMBRO_REMOTE_FILES_REGION` или `nocombro.remoteFiles.region`
- `NOCOMBRO_REMOTE_FILES_PREFIX` или `nocombro.remoteFiles.prefix`

Если конфиг не найден, приложение работает через `NoopRemoteFileStorageGateway`.

## Как подготовить `s3.properties`

1. Открыть `Yandex Cloud`.
2. Перейти в `IAM` -> `Сервисные аккаунты`.
3. Создать сервисный аккаунт, например `nocombro-storage-sa`.
4. Выдать ему роль `storage.editor`.
5. Внутри сервисного аккаунта создать `статический ключ доступа`.
6. Создать bucket в `Object Storage`.
7. Создать файл `s3.properties`.
8. Положить его сюда:

`C:\Users\<username>\AppData\Roaming\Nocombro\s3.properties`

## Что это даёт уже сейчас

- Можно хранить блобы файлов не только локально, но и в удалённом bucket.
- Внутри проекта появился отдельный слой object storage, а не зашивка провайдера прямо в UI.
- Провайдер можно поменять на любой S3-compatible без переделки формы файлов.

## Документация Yandex Cloud

- `Object Storage quickstart`: [yandex.cloud/en/docs/storage/quickstart](https://yandex.cloud/en/docs/storage/quickstart)
- `S3 API quickstart`: [yandex.cloud/en/docs/storage/s3/s3-api-quickstart](https://yandex.cloud/en/docs/storage/s3/s3-api-quickstart)
- `Static access key`: [yandex.cloud/en/docs/iam/operations/authentication/manage-access-keys](https://yandex.cloud/en/docs/iam/operations/authentication/manage-access-keys)
- `Pricing`: [yandex.cloud/en/docs/storage/pricing](https://yandex.cloud/en/docs/storage/pricing)

## Важно

- не класть `s3.properties` в репозиторий;
- не коммитить файл;
- не хранить секреты на рабочем столе;
- bucket лучше держать приватным, с доступом `С авторизацией`.

## Что ещё можно доделать позже

- уточнить UX-статусы наличия удалённой копии;
- отдельно продумать delete-логику;
- при необходимости добавить контроль версии локальной копии.

Сейчас это уже рабочая схема настройки remote files для desktop-приложения, а не только foundation.
