-- sql/init.sql

CREATE DATABASE IF NOT EXISTS `rag_kb` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `rag_kb`;

-- ==================== 用户表 ====================
CREATE TABLE `user` (
                        `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
                        `username`   VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
                        `password`   VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
                        `email`      VARCHAR(100) COMMENT '邮箱',
                        `role`       VARCHAR(20)  DEFAULT 'USER' COMMENT '角色: USER/ADMIN',
                        `status`     TINYINT      DEFAULT 1 COMMENT '1正常 0禁用',
                        `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 默认管理员（密码: fuckYourMom123@, BCrypt加密）
INSERT INTO `user` (`username`, `password`, `email`, `role`) VALUES
    ('admin', '$2b$12$PG5OU0ogEGnT8SuDLtoEYuHOsVwIA5OywQ9tIAjx3byreg/29KOKa', 'admin@ragkb.com', 'ADMIN');

-- ==================== 语雀知识库配置表 ====================
CREATE TABLE `yuque_repo` (
                              `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
                              `yuque_repo_id`   BIGINT        NOT NULL COMMENT '语雀知识库ID',
                              `yuque_repo_slug` VARCHAR(100)  NOT NULL UNIQUE COMMENT '语雀slug',
                              `name`            VARCHAR(200)  NOT NULL COMMENT '知识库名称',
                              `description`     VARCHAR(500)  COMMENT '描述',
                              `icon`            VARCHAR(500)  COMMENT '图标URL',
                              `sort_order`      INT           DEFAULT 0 COMMENT '排序',
                              `sync_status`     TINYINT       DEFAULT 0 COMMENT '0未同步 1同步中 2已完成 3失败',
                              `last_sync_at`    DATETIME      COMMENT '最后同步时间',
                              `last_sync_msg`   VARCHAR(500)  COMMENT '同步消息',
                              `doc_count`       INT           DEFAULT 0 COMMENT '文档数',
                              `chunk_count`     INT           DEFAULT 0 COMMENT '分块数',
                              `status`          TINYINT       DEFAULT 1 COMMENT '1启用 0禁用',
                              `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
                              `updated_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 语雀文档同步记录表 ====================
CREATE TABLE `yuque_doc` (
                             `id`              BIGINT        PRIMARY KEY AUTO_INCREMENT,
                             `yuque_repo_id`   BIGINT        NOT NULL COMMENT '关联yuque_repo.id',
                             `yuque_doc_id`    BIGINT        NOT NULL COMMENT '语雀文档ID',
                             `title`           VARCHAR(500)  NOT NULL COMMENT '文档标题',
                             `content_hash`    VARCHAR(64)   COMMENT '内容MD5哈希',
                             `word_count`      INT           COMMENT '字数',
                             `chunk_count`     INT           DEFAULT 0 COMMENT '分块数',
                             `sync_status`     TINYINT       DEFAULT 1 COMMENT '1成功 0失败',
                             `yuque_updated_at` DATETIME     COMMENT '语雀侧更新时间',
                             `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP,
                             `updated_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             UNIQUE KEY `uk_repo_doc` (`yuque_repo_id`, `yuque_doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 用户上传文档表 ====================
CREATE TABLE `user_document` (
                                 `id`          BIGINT        PRIMARY KEY AUTO_INCREMENT,
                                 `user_id`     BIGINT        NOT NULL COMMENT '上传用户ID',
                                 `file_name`   VARCHAR(255)  NOT NULL COMMENT '原始文件名',
                                 `file_type`   VARCHAR(20)   NOT NULL COMMENT 'pdf/txt/md',
                                 `file_size`   BIGINT        COMMENT '文件大小(字节)',
                                 `file_path`   VARCHAR(500)  COMMENT '存储路径',
                                 `title`       VARCHAR(500)  COMMENT '展示标题',
                                 `status`      TINYINT       DEFAULT 0 COMMENT '0待处理 1处理中 2就绪 3失败',
                                 `error_msg`   VARCHAR(500)  COMMENT '失败原因',
                                 `chunk_count` INT           DEFAULT 0,
                                 `word_count`  INT           DEFAULT 0,
                                 `is_global`   TINYINT       DEFAULT 0 COMMENT '是否全局文档(0否 1是) - 管理员可将就绪文档设为全局',
                                 `created_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 文档分块表 ====================
CREATE TABLE `document_chunk` (
                                  `id`          BIGINT        PRIMARY KEY AUTO_INCREMENT,
                                  `source_type` VARCHAR(20)   NOT NULL COMMENT 'yuque/user',
                                  `source_id`   BIGINT        NOT NULL COMMENT 'yuque_doc.id或user_document.id',
                                  `chunk_index` INT           NOT NULL COMMENT '块序号',
                                  `content`     TEXT          NOT NULL COMMENT '分块文本',
                                  `token_count` INT           COMMENT 'token数量',
                                  `vector_id`   VARCHAR(100)  COMMENT '向量数据库中的ID',
                                  `created_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP,
                                  INDEX idx_source (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 对话表 ====================
CREATE TABLE `conversation` (
                                `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
                                `user_id`    BIGINT       NOT NULL,
                                `title`      VARCHAR(200) COMMENT '对话标题',
                                `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
                                `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                INDEX idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 消息表 ====================
CREATE TABLE `message` (
                           `id`              BIGINT    PRIMARY KEY AUTO_INCREMENT,
                           `conversation_id` BIGINT    NOT NULL,
                           `role`            VARCHAR(20) NOT NULL COMMENT 'user/assistant',
                           `content`         TEXT      NOT NULL,
                           `references`      JSON      COMMENT '引用来源',
                           `token_used`      INT       COMMENT '消耗token数',
                           `created_at`      DATETIME  DEFAULT CURRENT_TIMESTAMP,
                           INDEX idx_conv_id (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
