package com.shikou.hannime.task;

import com.shikou.client.HanimeApiClient;
import com.shikou.exception.HanimeApiException;
import com.shikou.hannime.entities.domain.Video;
import com.shikou.hannime.entities.domain.VideoDownloadTask;
import com.shikou.hannime.service.TaskConfigService;
import com.shikou.hannime.service.VideoDownloadTaskService;
import com.shikou.hannime.service.VideoService;
import com.shikou.model.entities.SearchParams;
import com.shikou.model.entities.VideoInfo;
import com.shikou.model.entities.pages.SearchPage;
import com.shikou.model.entities.results.VideosResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "cache.enable", havingValue = "true", matchIfMissing = true)
public class VideoTaskScheduler {
    @Resource
    private TaskConfigService taskConfigService;
    @Resource
    private VideoService videoService;
    @Resource
    private VideoDownloadTaskService videoDownloadTaskService;
    @Resource
    private HanimeApiClient hanimeApiClient;

    private final ExecutorService executorService = Executors.newFixedThreadPool(16);

    /** 搜索请求间隔（毫秒），避免触发429限流 */
    private static final long SEARCH_INTERVAL_MS = 1500;

    private void sleepInterval() {
        try {
            Thread.sleep(SEARCH_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 每月1日凌晨1点全量扫描所有视频
     */
    @Scheduled(cron = "0 0 1 1 * ?")
    public void batchScanVideos(){
        scanVideosBySearch(0);
    }

    /**
     * 每天0点扫描前2页视频
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyScanVideos(){
        scanVideosBySearch(2);
    }

    /**
     * 通过搜索API分页获取视频的videoCode
     * @param maxPages 最大扫描页数，0表示扫描全部页面
     */
    private void scanVideosBySearch(int maxPages){
        log.info("开始通过搜索扫描视频");

        // 1. 获取第一页，拿到totalPage并立即添加任务
        SearchParams searchParams = SearchParams.builder()
                .sort("最新上市")
                .build();
        SearchPage searchPage;
        try {
            searchPage = hanimeApiClient.getSearchPage(searchParams);
        } catch (Exception e) {
            log.error("获取搜索首页失败", e);
            return;
        }

        int totalPage = searchPage.getTotalPage();
        if (maxPages > 0) {
            totalPage = Math.min(totalPage, maxPages);
        }
        List<String> pageCodes = searchPage.getVideos().stream()
                .map(VideoInfo::getVideoCode)
                .collect(Collectors.toList());
        log.info("搜索第1页完成，共{}页，获取{}个视频", totalPage, pageCodes.size());
        addTasksFromVideoCodes(pageCodes);

        // 2. 从第2页开始遍历剩余页面，每页扫描完立即添加任务
        for (int page = 2; page <= totalPage; page++) {
            sleepInterval();
            try {
                SearchParams pageParams = SearchParams.builder()
                        .sort("最新上市")
                        .page(page)
                        .build();
                VideosResult result = hanimeApiClient.search(pageParams);
                List<String> codes = result.getVideos().stream()
                        .map(VideoInfo::getVideoCode)
                        .collect(Collectors.toList());
                log.info("搜索第{}页完成，获取{}个视频", page, codes.size());
                addTasksFromVideoCodes(codes);
            } catch (Exception e) {
                log.warn("搜索第{}页失败", page, e);
            }
        }

        log.info("搜索扫描全部完成");
    }

    /**
     * 根据videoCode列表直接创建下载任务
     * <p>新版API支持直接通过videoCode和画质下载，无需提前获取视频详情
     */
    private void addTasksFromVideoCodes(List<String> videoCodes){
        List<VideoDownloadTask> tasks = new ArrayList<>();
        List<Video> videos = new ArrayList<>();

        String qualityConfig = taskConfigService.getQuality();
        log.info("开始创建任务，质量配置: {}，待处理视频数: {}", qualityConfig, videoCodes.size());

        for (String videoCode : videoCodes) {
            // 视频
            Video video = new Video();
            video.setVideoCode(videoCode);
            video.setQuality(qualityConfig);
            videos.add(video);

            VideoDownloadTask task = new VideoDownloadTask();
            task.setVideoCode(videoCode);
            task.setQuality(qualityConfig);
            tasks.add(task);
        }

        videoService.saveVideos(videos);
        videoDownloadTaskService.createTasks(tasks);
    }

    @Scheduled(fixedDelay = 900000)
    private void downloadVideos(){
        List<VideoDownloadTask> notCompletedTask = videoDownloadTaskService.getNotCompletedTask(5);
        notCompletedTask.forEach(task -> {
            executorService.submit(() -> {
                downloadVideo(task);
            });
        });
    }

    private void downloadVideo(VideoDownloadTask task){
        videoDownloadTaskService.processTask(task);
        String videoStorePath = taskConfigService.getVideoStorePath();
        File dir = new File(videoStorePath + task.getVideoCode());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        log.info("下载视频 {}", task.getVideoCode());

        String fileName = task.getVideoCode() + "_" + task.getQuality() + ".mp4";
        // 相当于系统的路径
        String filePath = task.getVideoCode() + File.separator + fileName;
        // 绝对路径
        String path = videoStorePath + filePath;
        File file = new File(path);
        try {
            String quality = task.getQuality();
            if(quality == null){
                quality = taskConfigService.getQuality();
            }
            hanimeApiClient.download(task.getVideoCode(), quality, file, (long downloaded, long total) -> {
                if(downloaded >= total){
                    log.info("下载完成 {}", task.getVideoCode());
                    videoDownloadTaskService.completeTask(task);
                    videoService.updateVideoPath(task.getVideoCode(), filePath);
                }
            });
        }catch (HanimeApiException e){
            videoDownloadTaskService.cancelTask(task);
            log.warn("下载视频 {} 取消", task.getVideoCode(), e);
        }catch (Exception e) {
            videoDownloadTaskService.failTask(task);
            log.warn("下载视频 {} 失败", task.getVideoCode(), e);
        }
    }

    @Scheduled(fixedDelay = 60000)
    private void recoverTask() {
        List<VideoDownloadTask> recoverTask = videoDownloadTaskService.getRecoverTask();
        recoverTask.forEach(task -> {
            executorService.submit(() -> {
                downloadVideo(task);
            });
        });
    }
}
