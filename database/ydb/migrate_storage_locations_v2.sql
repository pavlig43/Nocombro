-- Run manually before releasing a client that writes warehouse operations.
-- Columns stay nullable so existing YDB rows remain valid; clients map NULL to MAIN.

ALTER TABLE `batch_movement` ADD COLUMN `storage_location` Utf8;
ALTER TABLE `transact` ADD COLUMN `stock_operation_reason` Utf8;

-- Update every sync client before creating STORAGE_TRANSFER transactions.
