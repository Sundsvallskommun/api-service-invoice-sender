CREATE TABLE `batch_executions` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `started_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `completed_at` datetime(6) NOT NULL,
    `total_invoices` bigint(20) NOT NULL,
    `sent_invoices` bigint(20) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
