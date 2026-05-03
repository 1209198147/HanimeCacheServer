package com.shikou.hannime.util;

import com.shikou.hannime.entities.domain.Video;
import com.shikou.model.Artist;
import com.shikou.model.HanimeVideo;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class VideoConverter {
    /**
     * 将HanimeVideo转换为Video
     * videoCode 和 videoUrl , resolution 为null
     *
     * @param hanimeVideo HanimeVideo对象
     * @return
     */
    public static Video convert(HanimeVideo hanimeVideo) {
        Video video = new Video();
        video.setTitle(hanimeVideo.getTitle());
        video.setCoverUrl(hanimeVideo.getCoverUrl());
        video.setIntroduction(hanimeVideo.getIntroduction());
        List<String> tags = hanimeVideo.getTags();
        if(!CollectionUtils.isEmpty(tags)){
            StringBuilder sb = new StringBuilder();
            for (String tag : tags) {
                if(sb.isEmpty()){
                    sb.append(tag);
                }else {
                    sb.append(",").append(tag);
                }
            }
            video.setTags(sb.toString());
        }
        video.setUploadTime(hanimeVideo.getUploadTime());
        Artist artist = hanimeVideo.getArtist();
        if(artist != null){
            video.setUploader(artist.getName());
            video.setGenre(artist.getGenre());
        }
        return video;
    }
}
