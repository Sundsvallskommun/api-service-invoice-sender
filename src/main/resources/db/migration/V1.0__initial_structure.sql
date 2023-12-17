CREATE TABLE `batch_executions` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `basename` varchar(255) NOT NULL,
    `started_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `completed_at` datetime(6) NOT NULL,
    `total_invoices` bigint(20) NOT NULL,
    `sent_invoices` bigint(20) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- `invoice-sender`.batch_items definition

CREATE TABLE `batch_items` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `batch_id` int(11) NOT NULL,
    `filename` varchar(255) NOT NULL,
    `status` enum('NOT_AN_INVOICE','NOT_SENT','RECIPIENT_LEGAL_ID_FOUND','RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID','RECIPIENT_PARTY_ID_FOUND','RECIPIENT_PARTY_ID_NOT_FOUND','SENT','UNHANDLED') NOT NULL,
    PRIMARY KEY (`id`),
    KEY `fk_batch_item_batch_id` (`batch_id`),
    CONSTRAINT `fk_batch_item_batch_id` FOREIGN KEY (`batch_id`) REFERENCES `batch_executions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
