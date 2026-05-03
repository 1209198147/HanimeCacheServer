-- 创建video表
CREATE TABLE IF NOT EXISTS video (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    video_code VARCHAR(16) NOT NULL,
    video_url VARCHAR(256) NOT NULL,
    title VARCHAR(256),
    cover_url VARCHAR(1024),
    introduction VARCHAR(1024),
    tags VARCHAR(1024),
    upload_time VARCHAR(16),
    uploader VARCHAR(16),
    genre VARCHAR(8),
    resolution VARCHAR(16),
    path VARCHAR(1024)
);

-- 为video_code字段创建索引
CREATE INDEX IF NOT EXISTS idx_video_video_code ON video(video_code);

-- 创建video_download_task表
CREATE TABLE IF NOT EXISTS video_download_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    video_code VARCHAR(16) NOT NULL,
    video_url VARCHAR(256),
    status INTEGER DEFAULT 0,
    resolution VARCHAR(16),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 为video_download_task表的video_code字段创建索引
CREATE INDEX IF NOT EXISTS idx_video_download_task_video_code ON video_download_task(video_code);