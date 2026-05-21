package com.shikou.hannime;

import com.shikou.hannime.entities.domain.VideoDownloadTask;
import com.shikou.hannime.service.VideoDownloadTaskService;
import com.shikou.hannime.service.VideoService;
import com.shikou.hannime.task.VideoTaskScheduler;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HannimeApplicationTests {

    @Resource
    private VideoTaskScheduler videoTaskScheduler;
    @Resource
    private VideoDownloadTaskService taskService;

    @Test
    void contextLoads() {
    }

    @Test
    void testVideoTaskScheduler() {
        videoTaskScheduler.batchScanVideos();
    }

    @Test
    void createTask() {
        VideoDownloadTask task = new VideoDownloadTask();
        task.setVideoCode("406017");
        task.setQuality("1080P");
        taskService.createTask(task);
    }
}
