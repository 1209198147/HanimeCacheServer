package com.shikou.hannime.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shikou.hannime.dao.VideoDownloadTaskMapper;
import com.shikou.hannime.entities.domain.VideoDownloadTask;
import com.shikou.hannime.entities.enums.TaskStatus;
import com.shikou.hannime.exception.ErrorCode;
import com.shikou.hannime.util.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VideoDownloadTaskService extends ServiceImpl<VideoDownloadTaskMapper, VideoDownloadTask> {
    private final Set<Integer> NOT_COMPLETED_TASK_STATUS = Set.of(TaskStatus.PENDING.getCode(), TaskStatus.FAILED.getCode());

    public void createTask(VideoDownloadTask task){
        checkTask(task);
        task.setStatus(TaskStatus.PENDING.getCode());
        this.save(task);
    }

    private static void checkTask(VideoDownloadTask task) {
        AssertUtils.nonNull(task, "任务不能为空");
        AssertUtils.isNotEmpty(task.getVideoCode(), "视频码不能为空");
    }

    @Transactional(rollbackFor = Exception.class)
    public void createTasks(List<VideoDownloadTask> tasks){
        AssertUtils.nonNull(tasks, "任务不能为空");
        AssertUtils.isNotEmpty(tasks, "视频任务列表不能为空");
        int total = tasks.size();
        List<String> videoCodeList = tasks.stream().map(VideoDownloadTask::getVideoCode).collect(Collectors.toList());
        List<VideoDownloadTask> existTasks = this.lambdaQuery().in(VideoDownloadTask::getVideoCode, videoCodeList).in(VideoDownloadTask::getStatus, NOT_COMPLETED_TASK_STATUS).list();

        Set<String> existTasksSet = existTasks.stream()
                .map(task -> task.getVideoCode() + "_" + task.getResolution())
                .collect(Collectors.toSet());
        List<VideoDownloadTask> needCreate = tasks.stream()
                .filter(task -> !existTasksSet.contains(task.getVideoCode()))
                .map(task -> {
                    task.setStatus(TaskStatus.PENDING.getCode());
                    return task;
                }).collect(Collectors.toList());
        this.saveBatch(needCreate);
        log.info("创建任务完成，创建了 {} 个任务，共 {} 个任务已存在", needCreate.size(), total - needCreate.size());
    }

    public List<VideoDownloadTask> getNotCompletedTask(int count) {
        return this.query().in("status", NOT_COMPLETED_TASK_STATUS).last("LIMIT " + count).list();
    }

    public List<VideoDownloadTask> getRecoverTask() {
        return this.lambdaQuery()
                .eq(VideoDownloadTask::getStatus, TaskStatus.PROCESSING.getCode())
                .le(VideoDownloadTask::getUpdateTime, Date.from(Instant.now().minusSeconds(2*60*60)))
                .list();
    }

    public void processTask(VideoDownloadTask videoDownloadTask) {
        Integer id = videoDownloadTask.getId();
        String videoCode = videoDownloadTask.getVideoCode();
        VideoDownloadTask task = null;
        if (id != null){
            task = this.getById(id);
        }else if(videoCode != null){
            task = this.lambdaQuery().eq(VideoDownloadTask::getVideoCode, videoCode).one();
        }else{
            AssertUtils.fail(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        AssertUtils.nonNull(task, ErrorCode.TASK_NOT_FOUND, "任务不存在");

        TaskStatus taskStatus = TaskStatus.valueOf(task.getStatus());
        AssertUtils.isTrue(Set.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED)
                        .contains(taskStatus.getCode()),
                "当前任务无法处理 id:" + id + " 状态:" + taskStatus.getMessage());
        task.setStatus(TaskStatus.PROCESSING.getCode());
        boolean result = this.updateById(task);
        if (!result){
            log.error("任务状态无法改为处理中 id:{} videoCode:{}", id, videoCode);
        }
        log.info("任务处理中 id:{} videoCode:{}", id, videoCode);
    }

    public void completeTask(VideoDownloadTask videoDownloadTask) {
        Integer id = videoDownloadTask.getId();
        String videoCode = videoDownloadTask.getVideoCode();
        VideoDownloadTask task = null;
        if (id != null){
            task = this.getById(id);

        }else if(videoCode != null){
            task = this.lambdaQuery().eq(VideoDownloadTask::getVideoCode, videoCode).one();
        }else{
            AssertUtils.fail(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        AssertUtils.nonNull(task, ErrorCode.TASK_NOT_FOUND, "任务不存在");

        TaskStatus taskStatus = TaskStatus.valueOf(task.getStatus());
        AssertUtils.isTrue(TaskStatus.PROCESSING.equals(taskStatus),
                "当前任务无法完成 id:" + id + " 状态:" + taskStatus.getMessage());
        task.setStatus(TaskStatus.COMPLETED.getCode());
        boolean result = this.updateById(task);
        if (!result){
            log.error("任务完成失败 id:{} videoCode:{}", id, videoCode);
        }
        log.info("任务完成 id:{} videoCode:{}", id, videoCode);
    }

    public void failTask(VideoDownloadTask videoDownloadTask) {
        Integer id = videoDownloadTask.getId();
        String videoCode = videoDownloadTask.getVideoCode();
        VideoDownloadTask task = null;
        if (id != null){
            task = this.getById(id);
        }else if(videoCode != null){
            task = this.lambdaQuery().eq(VideoDownloadTask::getVideoCode, videoCode).one();
        }else{
            AssertUtils.fail(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        AssertUtils.nonNull(task, ErrorCode.TASK_NOT_FOUND, "任务不存在");

        TaskStatus taskStatus = TaskStatus.valueOf(task.getStatus());
        AssertUtils.isTrue(Set.of(TaskStatus.FAILED, TaskStatus.COMPLETED, TaskStatus.PENDING, TaskStatus.CANCELLED)
                        .contains(taskStatus.getCode()),
                "当前任务无法失败 id:" + id + " 状态:" + taskStatus.getMessage());
        task.setStatus(TaskStatus.FAILED.getCode());
        boolean result = this.updateById(task);
        if (!result){
            log.error("任务无法失败 id:{} videoCode:{}", id, videoCode);
        }
        log.info("任务失败 id:{} videoCode:{}", id, videoCode);
    }

    public void cancelTask(VideoDownloadTask videoDownloadTask) {
        Integer id = videoDownloadTask.getId();
        String videoCode = videoDownloadTask.getVideoCode();
        VideoDownloadTask task = null;
        if (id != null){
            task = this.getById(id);
        }else if(videoCode != null){
            task = this.lambdaQuery().eq(VideoDownloadTask::getVideoCode, videoCode).one();
        }else{
            AssertUtils.fail(ErrorCode.TASK_NOT_FOUND, "任务不存在");
        }
        AssertUtils.nonNull(task, ErrorCode.TASK_NOT_FOUND, "任务不存在");

        TaskStatus taskStatus = TaskStatus.valueOf(task.getStatus());
        AssertUtils.isTrue(Set.of(TaskStatus.CANCELLED, TaskStatus.COMPLETED)
                        .contains(taskStatus.getCode()),
                "当前任务无法取消 id:" + id + " 状态:" + taskStatus.getMessage());
        task.setStatus(TaskStatus.CANCELLED.getCode());
        boolean result = this.updateById(task);
        if (!result){
            log.error("任务无法取消 id:{} videoCode:{}", id, videoCode);
        }
        log.info("任务取消 id:{} videoCode:{}", id, videoCode);
    }
}
