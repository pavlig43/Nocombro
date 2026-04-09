# YDB Sync

## Текущее решение

- Удаленный backend для синхронизации: `YDB Serverless`.
- Локальная база остается в `Room/SQLite`.
- Синхронизация идет не на уровне файлов БД, а на уровне бизнес-сущностей с `syncId`.

## Что уже готово локально

- `sync_change` хранит локальную очередь изменений.
- `sync_state` хранит состояние последнего `push/pull`.
- Все syncable бизнес-таблицы имеют:
  - `syncId`
  - `updatedAt`
  - `deletedAt`
- Export и apply работают через стабильные `syncId`, а не через локальные `id`.

## Что считаем syncable сейчас

- `vendor`
- `document`
- `declaration`
- `product`
- `safety_stock`
- `composition`
- `product_declaration`
- `batch`
- `batch_movement`
- `transaction`
- `buy`
- `sale`
- `reminder`
- `expense`

## Что сейчас не входит в remote sync

- `file`
  Файлы сознательно отложены на отдельный контур через object storage.
- `batch_cost_price`
  Это вычисляемая локальная таблица, не удаленная истина.
- `pf`
  Это форма поверх `batch + batch_movement`, а не отдельная sync-таблица.

## Минимальная remote-схема

Пока используем одну универсальную таблицу `sync_push_log`.

Она хранит:
- источник изменения (`device_id`)
- тип сущности (`entity_table`)
- глобальный ключ сущности (`entity_sync_id`)
- тип изменения (`UPSERT` / `DELETE`)
- сериализованный payload (`payload_json`)
- технические поля очереди и курсора

SQL bootstrap лежит в:
- [sync_push_log.sql](/C:/Users/user/AndroidStudioProjects/Nocombro/database/ydb/sync_push_log.sql)

## Почему одна таблица пока нормально

- для `2-5` компьютеров и маленькой нагрузки это проще всего поднять и отладить
- push/pull уже реализованы под универсальный payload
- позже можно перейти к более предметной remote-схеме без переписывания локального sync-слоя

## Runtime-настройки

Для включения реального JDBC gateway:

- `NOCOMBRO_YDB_JDBC_URL` или JVM property `nocombro.ydb.jdbcUrl`
- `NOCOMBRO_YDB_TOKEN` или JVM property `nocombro.ydb.token`
- `NOCOMBRO_YDB_SYNC_TABLE` или JVM property `nocombro.ydb.syncTable`

Если `jdbcUrl` не задан, приложение работает через `YdbSyncGatewayMock`.

## Следующий практический шаг

1. Создать serverless БД в Yandex Cloud.
2. Подготовить таблицу `sync_push_log`.
3. Прописать `jdbcUrl`, `token` и имя таблицы.
4. Прогнать первый sync между двумя локальными базами.
