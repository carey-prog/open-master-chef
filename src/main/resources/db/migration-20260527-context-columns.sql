-- 扩大 recipe 上下文字段容量（智谱 WebSearch 高内容模式 JSON 可能超过 TEXT 64KB 上限）
-- 执行: mysql -h 127.0.0.1 -P 3306 -u root -p appdb < migration-20260527-context-columns.sql

USE appdb;

ALTER TABLE recipe
    MODIFY COLUMN rag_context MEDIUMTEXT NULL COMMENT 'RAG检索上下文',
    MODIFY COLUMN web_context MEDIUMTEXT NULL COMMENT '联网搜索上下文';
