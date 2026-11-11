-- ============================================================
-- 云枢智学 · 数据库建表脚本
-- 版本：1.0（2026-11-08，与 docs/design/03.数据库设计.md 对齐）
-- 数据库：yszx / MySQL 8.0 / InnoDB / utf8mb4
-- 约定：
--   1. 主键统一 id BIGINT UNSIGNED AUTO_INCREMENT
--   2. 禁止物理外键（本脚本无任何外键约束）
--   3. 业务表统一逻辑删除 delete_flag=0/1（operation_log、role_permission 例外）
--   4. 时间统一 UTC 存储，created_at/updated_at 自动维护
--   5. 状态字段取值见各列 COMMENT 与设计文档 §5
-- 注意：内置管理员密码为占位哈希，部署时重置，禁止直接用于登录
-- ============================================================

CREATE DATABASE IF NOT EXISTS `yszx`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `yszx`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 4.1 学员端账号
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `mobile` VARCHAR(11) NOT NULL COMMENT '手机号（登录账号）',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  `nickname` VARCHAR(50) NOT NULL COMMENT '用户名',
  `avatar` VARCHAR(500) NULL COMMENT '头像 URL',
  `email` VARCHAR(100) NULL COMMENT '邮箱（可空，非空值唯一）',
  `gender` TINYINT NOT NULL DEFAULT 0 COMMENT '0 保密 / 1 男 / 2 女',
  `preferred_subject_id` BIGINT UNSIGNED NULL COMMENT '技术方向 → subject.id',
  `guide_finished` TINYINT NOT NULL DEFAULT 0 COMMENT '新手引导是否看过 0/1',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 已启用 / 0 已禁用',
  `last_login_at` DATETIME NULL COMMENT '最后登录（UTC）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0/1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_mobile` (`mobile`),
  UNIQUE KEY `uk_student_email` (`email`),
  KEY `idx_student_created_at` (`created_at`),
  KEY `idx_student_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员';

-- ------------------------------------------------------------
-- 4.2 内容管理
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `subject`;
CREATE TABLE `subject` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT '学科名称',
  `icon` VARCHAR(50) NOT NULL COMMENT '图标（Emoji）',
  `description` VARCHAR(255) NOT NULL COMMENT '描述',
  `theme_color` VARCHAR(20) NOT NULL DEFAULT '#000000' COMMENT '主题色',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_name` (`name`),
  KEY `idx_subject_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学科';

DROP TABLE IF EXISTS `stage`;
CREATE TABLE `stage` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `subject_id` BIGINT UNSIGNED NOT NULL COMMENT '→ subject.id',
  `name` VARCHAR(50) NOT NULL COMMENT '阶段名称',
  `icon` VARCHAR(50) NULL COMMENT '图标（留空默认）',
  `sort_order` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_subject_name` (`subject_id`,`name`),
  KEY `idx_stage_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='阶段';

DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '课程名称',
  `cover_url` VARCHAR(500) NULL COMMENT '封面 URL',
  `intro` VARCHAR(500) NULL COMMENT '课程简介',
  `duration_minutes` INT NOT NULL DEFAULT 0 COMMENT '时长（分钟）',
  `source` TINYINT NOT NULL DEFAULT 1 COMMENT '1 B站 / 2 其他',
  `link` VARCHAR(500) NULL COMMENT '课程链接',
  `bvid` VARCHAR(20) NULL COMMENT 'BV 号（非 B 站来源为空）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 上架 / 2 下架',
  `subject_id` BIGINT UNSIGNED NULL COMMENT '主学科 → subject.id；NULL=其他学科',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_bvid` (`bvid`),
  KEY `idx_course_subject` (`subject_id`),
  KEY `idx_course_source` (`source`),
  KEY `idx_course_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='B站课程库';

DROP TABLE IF EXISTS `course_subject`;
CREATE TABLE `course_subject` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT UNSIGNED NOT NULL COMMENT '→ course.id',
  `subject_id` BIGINT UNSIGNED NOT NULL COMMENT '→ subject.id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_subject` (`course_id`,`subject_id`),
  KEY `idx_course_subject_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程-学科关联';

DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `stage_id` BIGINT UNSIGNED NOT NULL COMMENT '→ stage.id',
  `type` TINYINT NOT NULL COMMENT '1 单选 / 2 多选 / 3 判断 / 4 简答',
  `difficulty` TINYINT NOT NULL DEFAULT 2 COMMENT '1 简单 / 2 中等 / 3 困难',
  `content` TEXT NOT NULL COMMENT '题干',
  `answer` TEXT NULL COMMENT '判断题答案 / 简答参考答案；客观选择题为空',
  `analysis` TEXT NULL COMMENT '解析',
  `tags` VARCHAR(255) NULL COMMENT '标签，逗号分隔',
  `status` TINYINT NOT NULL DEFAULT 3 COMMENT '1 已上架 / 2 已下架 / 3 待审核 / 4 草稿（预留不启用）/ 5 已驳回',
  `reject_reason` VARCHAR(255) NULL COMMENT '驳回原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_question_stage` (`stage_id`),
  KEY `idx_question_status` (`status`),
  KEY `idx_question_type_difficulty` (`type`,`difficulty`),
  KEY `idx_question_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题库题目';

DROP TABLE IF EXISTS `question_option`;
CREATE TABLE `question_option` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT UNSIGNED NOT NULL COMMENT '→ question.id',
  `option_key` VARCHAR(4) NOT NULL COMMENT 'A/B/C/D',
  `option_text` VARCHAR(500) NOT NULL,
  `is_correct` TINYINT NOT NULL DEFAULT 0 COMMENT '1 正确 / 0 否',
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_option` (`question_id`,`option_key`),
  KEY `idx_question_option_qid` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目选项';

DROP TABLE IF EXISTS `interview_question`;
CREATE TABLE `interview_question` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `subject_id` BIGINT UNSIGNED NOT NULL COMMENT '方向 → subject.id（=学科）',
  `content` TEXT NOT NULL COMMENT '题干',
  `reference_answer` TEXT NULL COMMENT '参考答案',
  `topic` VARCHAR(50) NULL COMMENT '主题；NULL=未分类',
  `difficulty` TINYINT NOT NULL DEFAULT 2 COMMENT '1 简单 / 2 中等 / 3 困难',
  `tags` VARCHAR(255) NULL,
  `source_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1 文档解析 / 2 手工录入 / 3 手动导入',
  `source_doc_name` VARCHAR(255) NULL COMMENT '来源文档名',
  `source_seq` INT NULL COMMENT '文档内第 N 题',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 已上架 / 2 已下架',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_interview_question_subject_status` (`subject_id`,`status`),
  KEY `idx_interview_question_topic` (`topic`),
  KEY `idx_interview_question_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题库';

DROP TABLE IF EXISTS `interview_topic`;
CREATE TABLE `interview_topic` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `subject_id` BIGINT UNSIGNED NULL COMMENT '→ subject.id；NULL=全局主题',
  `name` VARCHAR(50) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_interview_topic_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题主题词表';

DROP TABLE IF EXISTS `learning_route`;
CREATE TABLE `learning_route` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `subject_id` BIGINT UNSIGNED NOT NULL COMMENT '关联学科（单学科）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
  `icon` VARCHAR(50) NULL,
  `target_education` VARCHAR(100) NULL COMMENT '适合学历',
  `description` TEXT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_learning_route_subject` (`subject_id`),
  KEY `idx_learning_route_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习路线';

DROP TABLE IF EXISTS `route_course`;
CREATE TABLE `route_course` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `route_id` BIGINT UNSIGNED NOT NULL COMMENT '→ learning_route.id',
  `course_id` BIGINT UNSIGNED NOT NULL COMMENT '→ course.id',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '课程顺序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route_course` (`route_id`,`course_id`),
  KEY `idx_route_course_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='路线-课程关联';

DROP TABLE IF EXISTS `route_course_completion`;
CREATE TABLE `route_course_completion` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT UNSIGNED NOT NULL COMMENT '→ student.id',
  `route_course_id` BIGINT UNSIGNED NOT NULL COMMENT '→ route_course.id',
  `completed_at` DATETIME NOT NULL COMMENT '完成时间（UTC）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route_course_completion` (`student_id`,`route_course_id`),
  KEY `idx_route_course_completion_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员课程完成';

-- ------------------------------------------------------------
-- 4.3 学习训练
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `practice_record`;
CREATE TABLE `practice_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT UNSIGNED NOT NULL COMMENT '→ student.id',
  `question_id` BIGINT UNSIGNED NOT NULL COMMENT '→ question.id',
  `mode` TINYINT NOT NULL DEFAULT 1 COMMENT '1 顺序 / 2 随机',
  `is_correct` TINYINT NOT NULL DEFAULT 0 COMMENT '1 对 / 0 错',
  `duration_seconds` INT NULL COMMENT '该题用时（秒）',
  `practiced_at` DATETIME NOT NULL COMMENT '作答时间（UTC）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_practice_record_student_time` (`student_id`,`practiced_at`),
  KEY `idx_practice_record_student_question` (`student_id`,`question_id`),
  KEY `idx_practice_record_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='刷题作答明细';

DROP TABLE IF EXISTS `wrong_question`;
CREATE TABLE `wrong_question` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT UNSIGNED NOT NULL COMMENT '→ student.id',
  `question_id` BIGINT UNSIGNED NOT NULL COMMENT '→ question.id',
  `error_count` INT NOT NULL DEFAULT 1 COMMENT '错误次数',
  `corrected_count` INT NOT NULL DEFAULT 0 COMMENT '连续答对次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 待复习 / 2 已掌握',
  `last_wrong_at` DATETIME NULL COMMENT '最近答错时间（UTC）',
  `last_answer` TEXT NULL COMMENT '最近一次错误作答快照',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wrong_question` (`student_id`,`question_id`),
  KEY `idx_wrong_question_student_status` (`student_id`,`status`),
  KEY `idx_wrong_question_last_wrong` (`last_wrong_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='错题本';

-- ------------------------------------------------------------
-- 4.4 AI 面试
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `interview_record`;
CREATE TABLE `interview_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT UNSIGNED NOT NULL COMMENT '→ student.id',
  `subject_id` BIGINT UNSIGNED NOT NULL COMMENT '面试方向 → subject.id',
  `difficulty` TINYINT NOT NULL COMMENT '1 简单 / 2 中等 / 3 困难',
  `question_count` INT NOT NULL COMMENT '题数 3/5/8/10/15',
  `overall_score` DECIMAL(3,1) NULL COMMENT '综合评分 0.0-10.0',
  `total_duration` INT NULL COMMENT '累计时长（秒）',
  `started_at` DATETIME NOT NULL COMMENT '开始时间（UTC）',
  `completed_at` DATETIME NOT NULL COMMENT '结束时间（UTC）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_interview_record_student_time` (`student_id`,`created_at`),
  KEY `idx_interview_record_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试记录';

DROP TABLE IF EXISTS `interview_question_score`;
CREATE TABLE `interview_question_score` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `interview_record_id` BIGINT UNSIGNED NOT NULL COMMENT '→ interview_record.id',
  `question_order` INT NOT NULL COMMENT '第 N 题（1 起）',
  `question_id` BIGINT UNSIGNED NOT NULL COMMENT '→ interview_question.id',
  `question_text` TEXT NOT NULL COMMENT '题干快照（回放展示）',
  `student_answer` TEXT NULL COMMENT '学员作答',
  `score` DECIMAL(3,1) NOT NULL COMMENT '本题得分 0.0-10.0',
  `ai_feedback` TEXT NULL COMMENT 'AI 评分理由',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_interview_score_record` (`interview_record_id`,`question_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试逐题评分';

DROP TABLE IF EXISTS `interview_report`;
CREATE TABLE `interview_report` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `interview_record_id` BIGINT UNSIGNED NOT NULL COMMENT '→ interview_record.id',
  `radar_json` TEXT NULL COMMENT '6 维能力分析 JSON',
  `strengths` TEXT NULL COMMENT '优点',
  `weaknesses` TEXT NULL COMMENT '待提升',
  `suggestions` TEXT NULL COMMENT '改进建议',
  `improvement_cards_json` TEXT NULL COMMENT '3 张提升卡 JSON',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_interview_report_record` (`interview_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试报告';

-- ------------------------------------------------------------
-- 4.5 系统管理
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号）',
  `password_hash` VARCHAR(100) NOT NULL,
  `mobile` VARCHAR(11) NULL COMMENT '手机号',
  `email` VARCHAR(100) NULL,
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '→ role.id',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 已启用 / 0 已禁用',
  `is_builtin` TINYINT NOT NULL DEFAULT 0 COMMENT '1 内置超级管理员',
  `last_login_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_username` (`username`),
  UNIQUE KEY `uk_admin_user_mobile` (`mobile`),
  KEY `idx_admin_user_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台用户';

DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(255) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色';

DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT UNSIGNED NULL COMMENT '父节点；NULL=组节点',
  `name` VARCHAR(50) NOT NULL,
  `code` VARCHAR(100) NOT NULL COMMENT '权限编码',
  `type` TINYINT NOT NULL COMMENT '1 组 / 2 操作点',
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`code`),
  KEY `idx_permission_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限';

DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '→ role.id',
  `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '→ permission.id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`),
  KEY `idx_role_permission_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-权限关联（覆盖式授权，无逻辑删除）';

DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '→ admin_user.id',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名快照',
  `action` VARCHAR(50) NOT NULL COMMENT '登录/登出/新增/修改/删除/禁用启用/导出/其他',
  `target_type` VARCHAR(50) NOT NULL COMMENT '对象类型',
  `target_name` VARCHAR(255) NOT NULL COMMENT '对象描述',
  `ip` VARCHAR(64) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_user` (`user_id`),
  KEY `idx_operation_log_time` (`created_at`),
  KEY `idx_operation_log_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志（审计，无逻辑删除）';

DROP TABLE IF EXISTS `system_setting`;
CREATE TABLE `system_setting` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(50) NOT NULL COMMENT '配置键',
  `config_value` TEXT NULL COMMENT '配置值（文本/JSON）',
  `remark` VARCHAR(255) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_flag` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_setting_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统设置';

-- ------------------------------------------------------------
-- 种子数据
-- ------------------------------------------------------------
INSERT INTO `subject` (`id`,`name`,`icon`,`description`,`theme_color`,`sort_order`) VALUES
  (1,'AI应用与智能体开发','🤖','从 Prompt 工程到 Agent 工作流，把大模型做成能落地的应用。','#0F6B57',1),
  (2,'AI大模型开发','🧠','模型微调、推理优化与训练数据工程，面向算法与平台岗位。','#27507A',2),
  (3,'AI具身智能机器人开发','🦾','感知、决策、控制一体化的具身智能系统实践。','#8A5A2B',3),
  (4,'AI嵌入式_机器人开发','🔧','端侧算力、传感器接入与实时系统的软硬协同。','#5B5B7A',4),
  (5,'AI测试','🧪','大模型应用的质量体系：评测集、回归与线上监控。','#7A4B5B',5),
  (6,'AI运维','🛠️','推理服务部署、成本控制与稳定性保障。','#3F6B4A',6),
  (7,'AI视觉设计','🎨','生成式视觉工作流与设计交付标准。','#A15C2E',7),
  (8,'AI视频与短剧创作','🎬','从分镜到成片的 AI 视频生产链路。','#6B4B8A',8);

INSERT INTO `role` (`id`,`name`,`code`,`description`) VALUES
  (1,'超级管理员','super_admin','全部权限'),
  (2,'内容审核','content_reviewer','查看全部内容 + 题库审核'),
  (3,'运营人员','operator','除系统管理外的全部操作');

INSERT INTO `permission` (`id`,`parent_id`,`name`,`code`,`type`,`sort_order`) VALUES
  (1,NULL,'题库管理','group:question',1,1),
  (2,NULL,'面试题库管理','group:interviewbank',1,2),
  (3,NULL,'用户管理','group:user',1,3),
  (4,NULL,'学习路线','group:route',1,4),
  (5,NULL,'学员管理','group:student',1,5),
  (6,NULL,'系统管理','group:system',1,6),
  (7,NULL,'学科管理','group:subject',1,7),
  (8,NULL,'B站课程库','group:course',1,8),
  (9,NULL,'数据看板','group:dashboard',1,9),
  (11,1,'查看','question:view',2,1),
  (12,1,'新增','question:add',2,2),
  (13,1,'编辑','question:edit',2,3),
  (14,1,'删除','question:delete',2,4),
  (15,1,'导入','question:import',2,5),
  (16,1,'导出','question:export',2,6),
  (17,1,'审核','question:audit',2,7),
  (21,2,'查看','interviewbank:view',2,1),
  (22,2,'上传','interviewbank:upload',2,2),
  (23,2,'编辑','interviewbank:edit',2,3),
  (24,2,'删除','interviewbank:delete',2,4),
  (25,2,'检索测试','interviewbank:search_test',2,5),
  (31,3,'查看','user:view',2,1),
  (32,3,'新增','user:add',2,2),
  (33,3,'编辑','user:edit',2,3),
  (34,3,'禁用','user:disable',2,4),
  (41,4,'查看','route:view',2,1),
  (42,4,'新增','route:add',2,2),
  (43,4,'编辑','route:edit',2,3),
  (44,4,'删除','route:delete',2,4),
  (51,5,'查看','student:view',2,1),
  (52,5,'编辑','student:edit',2,2),
  (53,5,'删除','student:delete',2,3),
  (61,6,'日志查看','system:log_view',2,1),
  (62,6,'系统设置','system:setting',2,2),
  (71,7,'查看','subject:view',2,1),
  (72,7,'新增','subject:add',2,2),
  (73,7,'编辑','subject:edit',2,3),
  (74,7,'删除','subject:delete',2,4),
  (75,8,'查看','course:view',2,1),
  (76,8,'新增','course:add',2,2),
  (77,8,'编辑','course:edit',2,3),
  (78,8,'删除','course:delete',2,4),
  (80,8,'导入','course:import',2,5),
  (79,9,'查看','dashboard:view',2,1);

-- 超级管理员：全部 35 项操作点
INSERT INTO `role_permission` (`role_id`,`permission_id`)
SELECT 1, p.`id` FROM `permission` p WHERE p.`type` = 2;

-- 内容审核：学科/课程库/路线查看 + 题库查看与审核 + 面试题库查看
INSERT INTO `role_permission` (`role_id`,`permission_id`) VALUES
  (2,71),(2,75),(2,41),(2,11),(2,17),(2,21);

-- 运营人员：除系统管理组（用户管理 31~34、日志 61、设置 62）与题库审核 17 外的全部操作点
INSERT INTO `role_permission` (`role_id`,`permission_id`)
SELECT 3, p.`id` FROM `permission` p
WHERE p.`type` = 2 AND p.`id` NOT IN (31,32,33,34,61,62,17);

INSERT INTO `system_setting` (`config_key`,`config_value`,`remark`) VALUES
  ('site_name','云枢智学','平台名称'),
  ('site_description','AI 赋能的智能面试训练平台','平台描述'),
  ('logo_url',NULL,'平台 Logo'),
  ('default_avatar',NULL,'默认头像');

-- 内置超级管理员：密码为占位哈希，部署初始化时重置（禁止直接登录）
INSERT INTO `admin_user` (`id`,`username`,`password_hash`,`mobile`,`email`,`role_id`,`status`,`is_builtin`) VALUES
  (1,'admin','$2a$10$00000000000000000000000000000000000000000000000000000',NULL,NULL,1,1,1);

SET FOREIGN_KEY_CHECKS = 1;
