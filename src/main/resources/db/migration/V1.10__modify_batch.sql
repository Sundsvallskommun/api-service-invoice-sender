ALTER TABLE `batch_executions` DROP COLUMN `local_path`;
ALTER TABLE `batch_executions` RENAME COLUMN `basename` TO `filename`;
