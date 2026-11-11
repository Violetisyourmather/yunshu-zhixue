-- ============================================================
-- 开发/联调测试学员（可选执行，勿在生产库执行）
-- 账号：13800138000 或 student@example.com
-- 密码：Student123
-- ============================================================
INSERT INTO `student` (`mobile`, `password_hash`, `nickname`, `email`, `gender`, `status`, `delete_flag`)
VALUES ('13800138000',
        '$2b$10$O6MGX7hJkzBEbatD0Y/VKusZJ7IwuSt9EQSbjtfUIijn9LWTwvYA6',
        '测试学员',
        'student@example.com',
        0, 1, 0)
ON DUPLICATE KEY UPDATE
  `password_hash` = VALUES(`password_hash`),
  `nickname` = VALUES(`nickname`),
  `email` = VALUES(`email`),
  `status` = 1,
  `delete_flag` = 0;
