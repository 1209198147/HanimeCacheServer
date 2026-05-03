package com.shikou.hannime.task;

import com.shikou.client.HanimeApiClient;
import com.shikou.exception.HanimeApiException;
import com.shikou.exception.HanimeNetworkException;
import com.shikou.hannime.config.VideoStoreConfig;
import com.shikou.hannime.entities.domain.Video;
import com.shikou.hannime.entities.domain.VideoDownloadTask;
import com.shikou.hannime.service.VideoDownloadTaskService;
import com.shikou.hannime.service.VideoService;
import com.shikou.hannime.util.AssertUtils;
import com.shikou.hannime.util.HanimeVideoUtils;
import com.shikou.hannime.util.VideoConverter;
import com.shikou.model.HanimeVideo;
import com.shikou.model.VideoQuality;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import kotlin.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VideoTaskScheduler {
    @Resource
    private VideoStoreConfig videoStoreConfig;
    @Resource
    private VideoService videoService;
    @Resource
    private VideoDownloadTaskService videoDownloadTaskService;
    @Resource
    private HanimeApiClient hanimeApiClient;

    private final ExecutorService executorService = Executors.newFixedThreadPool(16);

    @PostConstruct
    private void dobatchScanVideos(){
        batchScanVideos();
        downloadVideos();
    }

    @Scheduled(cron = "0 0 0 1 * ?")
    public void batchScanVideos(){
        int minVideoCode = videoStoreConfig.getMinVideoCode();
        int maxVideoCode = videoStoreConfig.getMaxVideoCode();
//        int count = maxVideoCode - minVideoCode + 1;
//        // 平均分为10批次，向上取整
//        int interval = (count + 9) / 10;
//
//        for(int i = 0 ; i < 10 ; i++){
//            int start = minVideoCode + i * interval;
//            int end = Math.min(maxVideoCode, start + interval - 1);
//            executorService.submit(() -> scanVideos(start, end));
//        }
        executorService.submit(() -> scanVideos(minVideoCode, maxVideoCode));
    }

    private void scanVideos(int minVideoCode, int maxVideoCode){
        List<VideoDownloadTask> tasks = new ArrayList<>();
        List<Video> videos = new ArrayList<>();
        log.info("开始扫描视频 {} - {}", minVideoCode, maxVideoCode);


        Set<String> filtered = videoService.lambdaQuery()
                .select(Video::getVideoCode, Video::getVideoUrl, Video::getResolution)
                .isNotNull(Video::getPath)
                .list().stream().map(video -> video.getVideoCode() + "_" + video.getResolution())
                .collect(Collectors.toSet());

        for(int i = minVideoCode ; i <= maxVideoCode ; i++){
            try{
                String videoCode = String.valueOf(i);
                String resolution = videoStoreConfig.getResolution();

                if(filtered.contains(videoCode + "_" + resolution)) {
                    log.info("视频 {} {} 已存在，跳过", videoCode, resolution);
                    continue;
                }

                HanimeVideo videoDetail = hanimeApiClient.getVideoDetail(videoCode);
                AssertUtils.nonNull(videoDetail, "无法获取视频详情 videCode: " + videoCode);
                VideoQuality quality = HanimeVideoUtils.getResolutionUrl(videoDetail, resolution);
                AssertUtils.nonNull(quality, "无法获取视频画质 videCode: " + videoCode + " resolution: " + resolution);
                AssertUtils.nonNull(quality.getUrl(), "无法获取视频画质 URL videCode: " + videoCode + " resolution: " + resolution);
                AssertUtils.nonNull(quality.getResolution(), "无法获取视频画质 resolution videCode: " + videoCode + " resolution: " + resolution);

                if(filtered.contains(videoCode + "_" + quality.getResolution())) {
                    log.info("视频 {} {} 已存在，跳过", videoCode, quality.getResolution());
                    continue;
                }

                // 视频
                Video video = VideoConverter.convert(videoDetail);
                video.setVideoCode(videoCode);
                video.setResolution(quality.getResolution());
                video.setVideoUrl(quality.getUrl());
                video.setResolution(resolution);
                videos.add(video);

                // 下载任务
                VideoDownloadTask task = new VideoDownloadTask();
                task.setVideoCode(videoCode);
                task.setVideoUrl(quality.getUrl());
                task.setResolution(quality.getResolution());
                tasks.add(task);
            }catch (HanimeNetworkException e){
                log.warn("网络异常无法获取视频详情 videCode: {}", i, e);
            }catch (Exception e){
                log.warn("无法获取视频详情 videCode: {}", i, e);
            }
        }

        videoService.saveVideos(videos);
        videoDownloadTaskService.createTasks(tasks);
    }

    @Scheduled(fixedDelay = 3600000)
    private void downloadVideos(){
        List<VideoDownloadTask> notCompletedTask = videoDownloadTaskService.getNotCompletedTask(10);
        notCompletedTask.forEach(task -> {
            executorService.submit(() -> {
                downloadVideo(task);
            });
        });
    }

    private void downloadVideo(VideoDownloadTask task){
        videoDownloadTaskService.processTask(task);
        String videoStorePath = videoStoreConfig.getVideoStorePath();
        File dir = new File(videoStorePath + task.getVideoCode());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        log.info("下载视频 {}", task.getVideoCode());

        String fileName = task.getVideoCode() + "_" + task.getResolution() + ".mp4";
        // 相当于系统的路径
        String filePath = task.getVideoCode() + File.separator + fileName;
        // 绝对路径
        String path = videoStorePath + filePath;
        File file = new File(path);
        try {
            String resolution = task.getResolution();
            if(resolution == null){
                resolution = videoStoreConfig.getResolution();
            }
            hanimeApiClient.download(task.getVideoCode(), resolution, file, (long downloaded, long total) -> {
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
}
