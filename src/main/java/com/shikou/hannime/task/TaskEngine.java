package com.shikou.hannime.task;

import com.shikou.client.HanimeApiClient;
import com.shikou.hannime.entities.domain.Video;
import com.shikou.hannime.entities.domain.VideoDownloadTask;
import com.shikou.hannime.entities.enums.EngineStatus;
import com.shikou.hannime.entities.enums.TaskStatus;
import com.shikou.hannime.service.TaskConfigService;
import com.shikou.hannime.service.VideoDownloadTaskService;
import com.shikou.hannime.service.VideoService;
import com.shikou.hannime.util.HanimeVideoUtils;
import com.shikou.model.entities.DownloadArgs;
import com.shikou.model.entities.DownloadInfo;
import com.shikou.model.entities.VideoQuality;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 任务引擎 —— 统一管理下载任务的调度、暂停、恢复、停止。
 *
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "video.task.enable", havingValue = "true", matchIfMissing = true)
public class TaskEngine {

    @Resource
    private TaskConfigService taskConfigService;
    @Resource
    private VideoService videoService;
    @Resource
    private VideoDownloadTaskService videoDownloadTaskService;
    @Resource
    private HanimeApiClient hanimeApiClient;

    // ==================== 配置 ====================

    @Value("${task.engine.thread-pool-size:16}")
    private int threadPoolSize;

    @Value("${task.engine.max-concurrent-downloads:5}")
    private int maxConcurrentDownloads;

    @Value("${task.engine.progress-flush-interval:5000}")
    private long progressFlushInterval;

    @Value("${task.engine.schedule-interval:60000}")
    private long scheduleInterval;

    @Value("${task.engine.recover-timeout:7200000}")
    private long recoverTimeout;

    // ==================== 状态 ====================

    /** 引擎全局状态 */
    private volatile EngineStatus engineStatus = EngineStatus.STOPPED;

    /** 下载线程池 */
    private ExecutorService executorService;

    /** 当前运行中的任务上下文 */
    private final ConcurrentHashMap<Integer, DownloadContext> runningTasks = new ConcurrentHashMap<>();

    /** 调度线程 */
    private Thread scheduleThread;

    /** 单次 DB 查询上限，避免全量加载 */
    private static final int FETCH_BATCH_SIZE = 100;

    // ==================== 生命周期 ====================

    @PostConstruct
    public synchronized void start() {
        if (engineStatus != EngineStatus.STOPPED) {
            log.info("TaskEngine 已在运行中，状态: {}", engineStatus);
            return;
        }
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        engineStatus = EngineStatus.RUNNING;
        log.info("TaskEngine 启动完成，线程池大小={}, 最大并发下载数={}", threadPoolSize, maxConcurrentDownloads);

        // 启动时恢复未完成任务
        recoverOnStartup();

        // 启动调度循环线程
        scheduleThread = new Thread(this::scheduleLoop, "task-engine-scheduler");
        scheduleThread.setDaemon(true);
        scheduleThread.start();
    }

    @PreDestroy
    public synchronized void shutdown() {
        log.info("TaskEngine 正在关闭...");
        engineStatus = EngineStatus.STOPPED;

        // 中断调度线程
        if (scheduleThread != null) {
            scheduleThread.interrupt();
        }

        // 取消所有运行中的任务
        for (DownloadContext ctx : runningTasks.values()) {
            ctx.cancelled = true;
            if (ctx.future != null) {
                ctx.future.cancel(true);
            }
        }
        runningTasks.clear();

        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("线程池未能在10秒内完全关闭");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("TaskEngine 已关闭");
    }

    // ==================== 全局控制 ====================

    /**
     * 暂停所有任务
     */
    public synchronized int pauseAll() {
        if (engineStatus == EngineStatus.PAUSED) {
            return 0;
        }
        engineStatus = EngineStatus.PAUSED;
        int count = 0;
        for (DownloadContext ctx : runningTasks.values()) {
            ctx.paused = true;
            if (ctx.future != null) {
                ctx.future.cancel(true);
            }
            count++;
        }
        // 批量更新 DB：所有 PROCESSING → PAUSED
        videoDownloadTaskService.updateStatusByStatus(TaskStatus.PAUSED.getCode(), TaskStatus.PROCESSING.getCode());
        log.info("TaskEngine 全局暂停，暂停了 {} 个任务", count);
        return count;
    }

    /**
     * 恢复所有任务
     */
    public synchronized int resumeAll() {
        engineStatus = EngineStatus.RUNNING;
        runningTasks.values().forEach(ctx -> ctx.paused = false);
        // 批量更新 DB：所有 PAUSED → PENDING，由调度循环重新拾取
        int count = videoDownloadTaskService.updateStatusByStatus(TaskStatus.PENDING.getCode(), TaskStatus.PAUSED.getCode());
        log.info("TaskEngine 全局恢复，恢复了 {} 个任务", count);
        return count;
    }

    /**
     * 停止所有任务
     */
    public synchronized int stopAll() {
        engineStatus = EngineStatus.STOPPED;
        int count = 0;
        for (DownloadContext ctx : runningTasks.values()) {
            ctx.cancelled = true;
            if (ctx.future != null) {
                ctx.future.cancel(true);
            }
            count++;
        }
        runningTasks.clear();
        // 批量更新 DB：所有 PROCESSING + PAUSED → CANCELLED
        videoDownloadTaskService.updateStatusByStatusList(
                TaskStatus.CANCELLED.getCode(),
                List.of(TaskStatus.PROCESSING.getCode(), TaskStatus.PAUSED.getCode())
        );
        log.info("TaskEngine 停止，取消了 {} 个运行中任务", count);
        return count;
    }

    // ==================== 单任务控制 ====================

    /**
     * 暂停指定任务
     */
    public void pauseTask(Integer taskId) {
        DownloadContext ctx = runningTasks.get(taskId);
        if (ctx == null) {
            log.warn("暂停任务 {} 失败：任务未在运行中", taskId);
            return;
        }
        ctx.paused = true;
        if (ctx.future != null) {
            ctx.future.cancel(true);
        }
        // 刷新进度到 DB
        flushProgress(ctx);
        videoDownloadTaskService.updateTaskStatus(taskId, TaskStatus.PAUSED.getCode());
        log.info("暂停任务 {} videoCode={}", taskId, ctx.videoCode);
    }

    /**
     * 恢复指定任务
     */
    public void resumeTask(Integer taskId) {
        VideoDownloadTask task = videoDownloadTaskService.getById(taskId);
        if (task == null) {
            log.warn("恢复任务 {} 失败：任务不存在", taskId);
            return;
        }
        TaskStatus status = TaskStatus.valueOf(task.getStatus());
        if (status != TaskStatus.PAUSED) {
            log.warn("恢复任务 {} 失败：当前状态为 {}", taskId, status.getMessage());
            return;
        }
        task.setStatus(TaskStatus.PENDING.getCode());
        videoDownloadTaskService.updateById(task);
        log.info("恢复任务 {} videoCode={}，已重置为 PENDING", taskId, task.getVideoCode());
    }

    /**
     * 取消指定任务（通过 TaskEngine，支持取消运行中任务）
     */
    public void cancelTask(Integer taskId) {
        DownloadContext ctx = runningTasks.get(taskId);
        if (ctx != null) {
            ctx.cancelled = true;
            if (ctx.future != null) {
                ctx.future.cancel(true);
            }
            flushProgress(ctx);
            runningTasks.remove(taskId);
            log.info("TaskEngine 取消运行中任务 {} videoCode={}", taskId, ctx.videoCode);
        }
        videoDownloadTaskService.updateTaskStatus(taskId, TaskStatus.CANCELLED.getCode());
    }

    // ==================== 查询方法 ====================

    public EngineStatus getEngineStatus() {
        return engineStatus;
    }

    public Set<Integer> getRunningTaskIds() {
        return new HashSet<>(runningTasks.keySet());
    }

    public int getRunningCount() {
        return runningTasks.size();
    }

    // ==================== 内部调度 ====================

    /**
     * 主调度循环，消费 PENDING 任务
     */
    private void scheduleLoop() {
        log.info("调度循环启动，间隔 {} ms", scheduleInterval);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(scheduleInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (engineStatus != EngineStatus.RUNNING) {
                continue;
            }

            try {
                // 1. 扫描超时 PROCESSING 任务，恢复调度
                recoverTimeoutTasks();

                // 2. 取 PENDING 任务 FIFO
                int available = maxConcurrentDownloads - runningTasks.size();
                if (available <= 0) {
                    continue;
                }

                List<VideoDownloadTask> pendingTasks = videoDownloadTaskService.lambdaQuery()
                        .eq(VideoDownloadTask::getStatus, TaskStatus.PENDING.getCode())
                        .orderByAsc(VideoDownloadTask::getCreateTime)
                        .last("LIMIT " + Math.min(available, FETCH_BATCH_SIZE))
                        .list();

                for (VideoDownloadTask task : pendingTasks) {
                    if (runningTasks.size() >= maxConcurrentDownloads) break;
                    submitTask(task);
                }
            } catch (Exception e) {
                log.error("调度循环异常", e);
            }
        }
        log.info("调度循环已退出");
    }

    /**
     * 恢复超时的 PROCESSING 任务（服务重启或线程异常后）
     */
    private void recoverTimeoutTasks() {
        try {
            Date timeoutThreshold = Date.from(Instant.now().minusMillis(recoverTimeout));
            List<VideoDownloadTask> timeoutTasks = videoDownloadTaskService.lambdaQuery()
                    .eq(VideoDownloadTask::getStatus, TaskStatus.PROCESSING.getCode())
                    .le(VideoDownloadTask::getUpdateTime, timeoutThreshold)
                    .list();
            for (VideoDownloadTask task : timeoutTasks) {
                log.info("恢复超时任务 id={} videoCode={}", task.getId(), task.getVideoCode());
                task.setStatus(TaskStatus.PENDING.getCode());
                videoDownloadTaskService.updateById(task);
            }
        } catch (Exception e) {
            log.error("恢复超时任务异常", e);
        }
    }

    /**
     * 启动时恢复未完成任务：PAUSED + PENDING → PENDING
     */
    private void recoverOnStartup() {
        try {
            // 恢复 PAUSED 任务
            int pausedCount = videoDownloadTaskService.updateStatusByStatus(
                    TaskStatus.PENDING.getCode(), TaskStatus.PAUSED.getCode());
            // 恢复 PROCESSING 任务（服务异常关闭遗留）
            int processingCount = videoDownloadTaskService.updateStatusByStatus(
                    TaskStatus.PENDING.getCode(), TaskStatus.PROCESSING.getCode());
            if (pausedCount > 0 || processingCount > 0) {
                log.info("启动恢复：PAUSED→PENDING {} 个, PROCESSING→PENDING {} 个", pausedCount, processingCount);
            }
        } catch (Exception e) {
            log.error("启动恢复任务异常", e);
        }
    }

    // ==================== 任务提交与执行 ====================

    /**
     * 提交单个任务到线程池
     */
    private void submitTask(VideoDownloadTask task) {
        try {
            videoDownloadTaskService.processTask(task);
        } catch (Exception e) {
            log.warn("任务状态切换失败 id={}", task.getId(), e);
            return;
        }

        DownloadContext ctx = new DownloadContext();
        ctx.taskId = task.getId();
        ctx.videoCode = task.getVideoCode();
        ctx.quality = task.getQuality();
        runningTasks.put(task.getId(), ctx);

        Future<?> future = executorService.submit(() -> executeDownload(ctx));
        ctx.future = future;
        log.debug("任务已提交 id={} videoCode={}", task.getId(), task.getVideoCode());
    }

    /**
     * 执行下载逻辑，包含协作式暂停/停止检测
     */
    private void executeDownload(DownloadContext ctx) {
        VideoDownloadTask task = new VideoDownloadTask();
        task.setId(ctx.taskId);
        task.setVideoCode(ctx.videoCode);

        try {
            String videoStorePath = taskConfigService.getVideoStorePath();
            File dir = new File(videoStorePath + ctx.videoCode);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String quality = ctx.quality;
            if (quality == null) {
                quality = taskConfigService.getQuality();
            }

            DownloadInfo downloadInfo = hanimeApiClient.getDownloadInfo(ctx.videoCode);
            VideoQuality videoQuality = HanimeVideoUtils.resolveQuality(downloadInfo, quality);
            String downloadQuality = Optional.ofNullable(videoQuality).map(VideoQuality::getQuality).orElse(null);
            String downloadUrl = Optional.ofNullable(videoQuality).map(VideoQuality::getUrl).orElse(null);
            String suffix = Optional.ofNullable(videoQuality).map(VideoQuality::getSuffix).orElse(null);

            String fileName = ctx.videoCode + "_" + downloadQuality;
            String downloadDir = Path.of(videoStorePath, ctx.videoCode).toString();

            log.info("开始下载 {} 画质={}", ctx.videoCode, downloadQuality);

            // 构造下载参数
            DownloadArgs downloadArgs = DownloadArgs.builder()
                    .downloadUrl(downloadUrl)
                    .downloadDir(downloadDir)
                    .videoName(fileName)
                    .suffix(suffix)
                    .build();

            hanimeApiClient.download(downloadArgs, (downloaded, total) -> {
                // 更新进度
                ctx.currentBytes.set(downloaded);
                ctx.totalBytes.set(total);

                // 协作式检查：暂停
                if (ctx.paused || engineStatus == EngineStatus.PAUSED) {
                    flushProgress(ctx);
                    // 等待恢复
                    while ((ctx.paused || engineStatus == EngineStatus.PAUSED)
                            && !ctx.cancelled
                            && engineStatus != EngineStatus.STOPPED) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }

                // 协作式检查：取消/停止
                if (ctx.cancelled || engineStatus == EngineStatus.STOPPED) {
                    log.info("下载被中断 {} 取消={} 引擎停止={}",
                            ctx.videoCode, ctx.cancelled, engineStatus == EngineStatus.STOPPED);
                    throw new RuntimeException("Download interrupted");
                }

                // 定期刷库
                long now = System.currentTimeMillis();
                if (now - ctx.lastFlushTime > progressFlushInterval) {
                    flushProgress(ctx);
                    ctx.lastFlushTime = now;
                }

                // 下载完成
                if (downloaded >= total) {
                    flushProgress(ctx);
                    String filePath = ctx.videoCode + "/" + fileName + "." + suffix;
                    videoDownloadTaskService.completeTask(task);
                    // 视频
                    Video video = new Video();
                    video.setVideoCode(ctx.videoCode);
                    video.setQuality(downloadQuality);
                    video.setPath(filePath);
                    videoService.save(video);
                    log.info("下载完成 {} 文件={}", ctx.videoCode, filePath);
                }
            });

        } catch (Exception e) {
            log.warn("下载失败 {} error={}", ctx.videoCode, e.getMessage());
            videoDownloadTaskService.failTaskWithMessage(task, e.getMessage());
        } finally {
            runningTasks.remove(ctx.taskId);
        }
    }

    /**
     * 刷新进度到数据库
     */
    private void flushProgress(DownloadContext ctx) {
        try {
            videoDownloadTaskService.updateProgress(
                    ctx.taskId, ctx.currentBytes.get(), ctx.totalBytes.get());
        } catch (Exception e) {
            log.warn("进度刷库失败 taskId={}", ctx.taskId, e);
        }
    }

    // ==================== 内部数据结构 ====================

    /**
     * 下载上下文，承载运行中任务的所有状态
     */
    static class DownloadContext {
        Integer taskId;
        String videoCode;
        String quality;
        volatile boolean paused;
        volatile boolean cancelled;
        AtomicLong currentBytes = new AtomicLong(0);
        AtomicLong totalBytes = new AtomicLong(0);
        Future<?> future;
        long lastFlushTime;
    }
}
