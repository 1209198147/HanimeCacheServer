package com.shikou.hannime.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shikou.hannime.config.VideoStoreConfig;
import com.shikou.hannime.dao.VideoMapper;
import com.shikou.hannime.entities.domain.Video;
import com.shikou.hannime.entities.response.VideoResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VideoService extends ServiceImpl<VideoMapper, Video> {
    @Resource
    private VideoStoreConfig videoStoreConfig;

    public VideoResponse getVideo(String videoCode){
        List<Video> videos = this.lambdaQuery().eq(Video::getVideoCode, videoCode)
                .isNotNull(Video::getPath)
                .list();
        if(!CollectionUtils.isEmpty(videos)){
            VideoResponse videoResponse = VideoResponse.fromVideo(videos.get(0));
            Map<String, VideoResponse.HanimeLink> videoUrls = videos.stream()
                    .collect(Collectors.toMap(Video::getResolution, video -> {
                        String path = video.getPath();
                        String suffix = getSuffix(path);
                        String subtype = getSubtype(suffix);
                        String link = "/resource/getVideo?videoCode=" + video.getVideoCode() + "&resolution=" + video.getResolution();
                        return VideoResponse.HanimeLink.builder()
                                .link(link)
                                .subtype(subtype)
                                .suffix(suffix)
                                .build();
                    }));
            videoResponse.setVideoUrls(videoUrls);
            return videoResponse;
        }
        return null;
    }

    private String getSuffix(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * 根据文件后缀获取subtype
     *
     * @param suffix
     * @return
     */
    private String getSubtype(String suffix) {
        return switch (suffix) {
            case "mp4" -> "mp4";
            case "mpeg" -> "mpeg";
            case "avi" -> "x-msvideo";
            case "3gp" -> "3gpp";
            case "3g2" -> "3gpp2";
            case "ogv" -> "ogg";
            case "ts" -> "mp2t";
            case "webm" -> "webm";
            default -> "mp4";
        };
    }

    /**
     * 根据视频代码和分辨率查询视频
     * @param videoCode 视频代码
     * @return Video对象
     */
    public List<Video> getVideoByCode(String videoCode) {
        return this.lambdaQuery().eq(Video::getVideoCode, videoCode)
                .select(Video::getVideoCode, Video::getResolution, Video::getPath)
                .isNotNull(Video::getPath)
                .list();
    }

    public boolean updateVideoPath(String videoCode, String path){
        return this.lambdaUpdate().eq(Video::getVideoCode, videoCode)
                .set(Video::getPath, path)
                .update();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveVideos(List<Video> videos){
        Set<String> videoCodes = videos.stream().map(Video::getVideoCode).collect(Collectors.toSet());
        Map<String, Integer> map = this.lambdaQuery().in(Video::getVideoCode, videoCodes).select(Video::getVideoCode, Video::getResolution)
                .list().stream()
                .collect(Collectors.toMap(video -> video.getVideoCode() + "_" + video.getResolution(), video -> video.getId()));

        List<Video> needUpdate = new ArrayList<>();
        List<Video> needInsert = new ArrayList<>();

        for (Video video : videos) {
            String key = video.getVideoCode() + "_" + video.getResolution();
            Integer id = map.getOrDefault(key, null);

            if(id == null){
                needInsert.add(video);
            }else{
                video.setId(id);
                needUpdate.add(video);
            }
        }
        this.updateBatchById(needUpdate);
        log.info("更新了{}条数据", needUpdate.size());
        this.saveBatch(needInsert);
        log.info("插入了{}条数据", needInsert.size());
    }
}
