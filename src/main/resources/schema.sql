-- 创建video表
CREATE TABLE IF NOT EXISTS video (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    video_code VARCHAR(16) NOT NULL,
    quality VARCHAR(16),
    path VARCHAR(1024)
);

-- 为video_code字段创建索引
CREATE INDEX IF NOT EXISTS idx_video_video_code ON video(video_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_video_video_code_quality ON video(video_code, quality);

-- 创建video_download_task表
CREATE TABLE IF NOT EXISTS video_download_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    video_code VARCHAR(16) NOT NULL,
    status INTEGER DEFAULT 0,
    quality VARCHAR(16),
    current_bytes INTEGER DEFAULT 0,
    total_bytes INTEGER DEFAULT 0,
    error_message VARCHAR(512),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 为video_download_task表的video_code和quality字段创建索引
CREATE INDEX IF NOT EXISTS idx_video_download_task_video_code_quality ON video_download_task(video_code, quality);

-- 核心调度索引：按状态 + 创建时间排序，取最早一批 PENDING 任务
CREATE INDEX IF NOT EXISTS idx_task_status_create_time ON video_download_task(status, create_time);