CREATE TABLE `files` (
    `data` longblob NOT NULL,
    `filename` varchar(255) NOT NULL,
    `batch_id` int(11) NOT NULL,
    PRIMARY KEY (`batch_id`),
    CONSTRAINT `FKtoyogiia4w7wd0o6gkpij30u9` FOREIGN KEY (`batch_id`) REFERENCES `batch_executions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;