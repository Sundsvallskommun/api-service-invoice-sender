INSERT INTO batch_executions(id, basename, started_at, completed_at, total_invoices, ignored_invoices, sent_invoices,
                             municipality_id, target_path, local_path, archive_path, processing_enabled, completed,
                             data, date, batch_status)
VALUES (1, 'batch1', '2024-01-01 00:00:00.00', '2024-01-01 00:01:00.00', 7, 1, 5, 2281, 'targetpath', 'localpath',
        'archivepath', 1, 0, NULL, '2024-01-01', 'READY'),
       (2, 'batch2', '2024-01-01 00:00:00.00', '2024-01-01 00:01:00.00', 7, 1, 5, 2281, 'targetpath', 'localpath',
        'archivepath', 1, 0, NULL, '2024-01-01', 'READY'),
       (3, 'batch3', '2024-01-01 00:00:00.00', '2024-01-01 00:01:00.00', 7, 1, 5, 2281, 'targetpath', 'localpath',
        'archivepath', 1, 0, NULL, '2024-01-01', 'MANAGED'),
       (4, 'batch4', '2024-01-01 00:00:00.00', '2024-01-01 00:01:00.00', 7, 1, 5, 2281, 'targetpath', 'localpath',
        'archivepath', 1, 0, NULL, '2024-01-01', 'MANAGED');



INSERT INTO batch_items(id, batch_id, filename, status, type)
VALUES (1, 1, 'filename1', 'SENT', 'INVOICE'),
       (2, 1, 'filename2', 'SENT', 'INVOICE'),
       (3, 1, 'filename3', 'SENT', 'INVOICE'),
       (4, 1, 'filename4', 'NOT_SENT', 'INVOICE'),
       (5, 1, 'filename5', 'NOT_SENT', 'INVOICE'),
       (6, 1, 'filename6', 'NOT_SENT', 'UNKNOWN'),
       (7, 1, 'filename7', 'SENT', 'INVOICE'),
       (8, 1, 'filename8', 'SENT', 'INVOICE'),

       (9, 2, 'filename9', 'SENT', 'INVOICE'),
       (10, 2, 'filename10', 'SENT', 'INVOICE'),
       (11, 2, 'filename11', 'SENT', 'INVOICE'),
       (12, 2, 'filename12', 'NOT_SENT', 'INVOICE'),
       (13, 2, 'filename13', 'NOT_SENT', 'INVOICE'),
       (14, 2, 'filename14', 'NOT_SENT', 'UNKNOWN'),
       (15, 2, 'filename15', 'SENT', 'INVOICE'),
       (16, 2, 'filename16', 'SENT', 'INVOICE'),

       (17, 3, 'filename17', 'SENT', 'INVOICE'),
       (18, 3, 'filename18', 'SENT', 'INVOICE'),
       (19, 3, 'filename19', 'SENT', 'INVOICE'),
       (20, 3, 'filename20', 'NOT_SENT', 'INVOICE'),
       (21, 3, 'filename21', 'NOT_SENT', 'INVOICE'),
       (22, 3, 'filename22', 'NOT_SENT', 'UNKNOWN'),
       (23, 3, 'filename23', 'SENT', 'INVOICE'),
       (24, 3, 'filename24', 'SENT', 'INVOICE'),

       (25, 4, 'filename25', 'SENT', 'INVOICE'),
       (26, 4, 'filename26', 'SENT', 'INVOICE'),
       (27, 4, 'filename27', 'SENT', 'INVOICE'),
       (28, 4, 'filename28', 'NOT_SENT', 'INVOICE'),
       (29, 4, 'filename29', 'NOT_SENT', 'INVOICE'),
       (30, 4, 'filename30', 'NOT_SENT', 'UNKNOWN'),
       (31, 4, 'filename31', 'SENT', 'INVOICE'),
       (32, 4, 'filename32', 'SENT', 'INVOICE');