package com.shikou.hannime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shikou.hannime.entities.domain.VideoDownloadTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface VideoDownloadTaskMapper extends BaseMapper<VideoDownloadTask> {

    /**
     * 按状态分组统计任务数量，用于高性能状态统计
     */
    List<Map<String, Object>> countByStatus();

    /**
     * 批量更新指定状态的任务为目标状态
     */
    int updateStatusByStatus(Integer targetStatus, Integer sourceStatus);

    /**
     * 批量更新指定状态列表的任务为目标状态
     */
    int updateStatusByStatusList(Integer targetStatus, List<Integer> sourceStatusList);
}
