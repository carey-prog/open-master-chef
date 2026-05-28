-- 一饭封神 数据库初始化脚本
-- Docker 容器 mysql8，默认库 appdb
-- 执行: mysql -h 127.0.0.1 -P 3306 -u root -p < schema.sql

USE appdb;

-- 食材分类
CREATE TABLE IF NOT EXISTS ingredient_category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name        VARCHAR(50)  NOT NULL COMMENT '分类名称',
    emoji       VARCHAR(20)  DEFAULT NULL COMMENT '分类图标',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材分类';

-- 食材库（快速选择）
CREATE TABLE IF NOT EXISTS ingredient (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '食材ID',
    category_id BIGINT       NOT NULL COMMENT '分类ID',
    name        VARCHAR(100) NOT NULL COMMENT '食材名称',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category_id),
    INDEX idx_name (name),
    CONSTRAINT fk_ingredient_category FOREIGN KEY (category_id) REFERENCES ingredient_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材库';

-- 菜系
CREATE TABLE IF NOT EXISTS cuisine (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜系ID',
    code        VARCHAR(50)  NOT NULL COMMENT '菜系编码',
    name        VARCHAR(50)  NOT NULL COMMENT '菜系名称',
    emoji       VARCHAR(20)  DEFAULT NULL COMMENT '图标',
    group_type  VARCHAR(20)  NOT NULL DEFAULT 'chinese' COMMENT 'chinese/international',
    sort_order  INT          DEFAULT 0,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜系';

-- 菜谱主表
CREATE TABLE IF NOT EXISTS recipe (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜谱ID',
    session_id      VARCHAR(64)  DEFAULT NULL COMMENT 'Agent会话ID',
    title           VARCHAR(200) NOT NULL COMMENT '菜名',
    cuisine_code    VARCHAR(50)  DEFAULT NULL COMMENT '菜系编码',
    cuisine_name    VARCHAR(50)  DEFAULT NULL COMMENT '菜系名称',
    difficulty      VARCHAR(20)  DEFAULT '简单' COMMENT '难度',
    cooking_time    INT          DEFAULT 30 COMMENT '烹饪时间(分钟)',
    custom_require  TEXT         DEFAULT NULL COMMENT '自定义要求',
    summary         TEXT         DEFAULT NULL COMMENT '简介',
    image_url       VARCHAR(500) DEFAULT NULL COMMENT '效果图URL',
    source_image    VARCHAR(500) DEFAULT NULL COMMENT '用户上传识别图',
    input_ingredients JSON       DEFAULT NULL COMMENT '用户输入食材',
    rag_context     MEDIUMTEXT   DEFAULT NULL COMMENT 'RAG检索上下文',
    web_context     MEDIUMTEXT   DEFAULT NULL COMMENT '联网搜索上下文',
    nutrition_json  JSON         DEFAULT NULL COMMENT '营养分析JSON',
    status          VARCHAR(20)  DEFAULT 'completed' COMMENT '状态',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_cuisine (cuisine_code),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱';

-- 菜谱食材明细
CREATE TABLE IF NOT EXISTS recipe_ingredient (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id   BIGINT       NOT NULL COMMENT '菜谱ID',
    category    VARCHAR(20)  NOT NULL DEFAULT '主料' COMMENT '主料/配菜/调料',
    name        VARCHAR(100) NOT NULL COMMENT '食材名',
    amount      VARCHAR(50)  DEFAULT NULL COMMENT '用量',
    sort_order  INT          DEFAULT 0,
    INDEX idx_recipe (recipe_id),
    CONSTRAINT fk_recipe_ingredient FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱食材';

-- 菜谱步骤
CREATE TABLE IF NOT EXISTS recipe_step (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id   BIGINT       NOT NULL COMMENT '菜谱ID',
    step_number INT          NOT NULL COMMENT '步骤序号',
    description TEXT         NOT NULL COMMENT '步骤描述',
    INDEX idx_recipe (recipe_id),
    CONSTRAINT fk_recipe_step FOREIGN KEY (recipe_id) REFERENCES recipe(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜谱步骤';

-- Agent 会话状态（可选持久化备份）
CREATE TABLE IF NOT EXISTS agent_session (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id  VARCHAR(64)  NOT NULL COMMENT '会话ID',
    state_json  JSON         DEFAULT NULL COMMENT '状态快照',
    status      VARCHAR(20)  DEFAULT 'running' COMMENT 'running/completed/failed',
    error_msg   TEXT         DEFAULT NULL,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent会话';
