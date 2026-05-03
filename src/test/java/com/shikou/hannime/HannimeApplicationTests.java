package com.shikou.hannime;

import com.shikou.hannime.task.VideoTaskScheduler;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HannimeApplicationTests {

    @Resource
    private VideoTaskScheduler videoTaskScheduler;

    @Test
    void contextLoads() {
    }

    @Test
    void testVideoTaskScheduler() {
        videoTaskScheduler.batchScanVideos();
    }
}
