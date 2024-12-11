ALTER TABLE `batch_executions`
    ADD COLUMN `target_path` VARCHAR(255);
ALTER TABLE `batch_executions`
    ADD COLUMN `local_path` VARCHAR(255);
ALTER TABLE `batch_executions`
    ADD COLUMN `archive_path` VARCHAR(255);
ALTER TABLE `batch_executions`
    ADD COLUMN `processing_enabled` BIT NOT NULL DEFAULT 0;
ALTER TABLE `batch_executions`
    ADD COLUMN `completed` BIT NOT NULL DEFAULT 1;
ALTER TABLE `batch_executions`
    ADD COLUMN `data` LONGBLOB;
ALTER TABLE `batch_items`
    ADD COLUMN `type` VARCHAR(255);

ALTER TABLE `batch_executions`
    ALTER COLUMN `processing_enabled` DROP DEFAULT;
ALTER TABLE `batch_executions`
    ALTER COLUMN `completed` DROP DEFAULT;

UPDATE `batch_items`
SET `type` = 'UNKNOWN'
WHERE `type` IS NULL;
