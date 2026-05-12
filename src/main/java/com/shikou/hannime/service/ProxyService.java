package com.shikou.hannime.service;

import com.shikou.client.HanimeApiClient;
import com.shikou.exception.HanimeApiException;
import com.shikou.exception.HanimeNetworkException;
import com.shikou.hannime.entities.domain.Video;
import com.shikou.hannime.exception.BizException;
import com.shikou.hannime.exception.ErrorCode;
import com.shikou.model.entities.*;
import com.shikou.model.entities.pages.*;
import com.shikou.model.entities.results.PlaylistsResult;
import com.shikou.model.entities.results.VideosResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 代理服务：封装对 HanimeApiClient 的调用，
 * 并在上游响应中替换视频 URL 为本地缓存地址
 */
@Slf4j
@Service
public class ProxyService {

    @Resource
    private HanimeApiClient hanimeApiClient;

    @Resource
    private VideoService videoService;

    // ==================== 首页 ====================

    /**
     * 获取首页数据，替换 sections 中各 videoInfoList 的视频 URL
     */
    public HomePage getHomePage() {
        HomePage homePage;
        try {
            homePage = hanimeApiClient.getHomePage();
        } catch (HanimeApiException e) {
            log.error("获取首页失败: {}", e.getMessage());
            throw new BizException(ErrorCode.FAIL, "获取首页数据失败: " + e.getMessage());
        } catch (HanimeNetworkException e) {
            log.error("获取首页网络异常: {}", e.getMessage());
            throw new BizException(ErrorCode.FAIL, "网络异常，请稍后重试");
        }
        if (homePage != null && !CollectionUtils.isEmpty(homePage.getSections())) {
            for (HomePageSection section : homePage.getSections()) {
                replaceVideoUrlsInVideoInfoList(section.getVideoInfoList());
            }
        }
        return homePage;
    }

    // ==================== 搜索页 ====================

    /**
     * 获取搜索页（首次，含筛选条件）
     */
    public SearchPage getSearchPage(SearchParams params) {
        SearchPage searchPage;
        try {
            searchPage = hanimeApiClient.getSearchPage(params);
        } catch (HanimeApiException e) {
            log.error("获取搜索页失败: {}", e.getMessage());
            throw new BizException(ErrorCode.FAIL, "获取搜索数据失败: " + e.getMessage());
        } catch (HanimeNetworkException e) {
            log.error("获取搜索页网络异常: {}", e.getMessage());
            throw new BizException(ErrorCode.FAIL, "网络异常，请稍后重试");
        }
        if (searchPage != null) {
            replaceVideoUrlsInVideoInfoList(searchPage.getVideos());
        }
        return searchPage;
    }

    /**
     * 搜索视频（翻页/后续搜索，仅视频列表 + 分页）
     */
    public VideosResult search(SearchParams params) {
        VideosResult result;
        try {
            result = hanimeApiClient.search(params);
        } catch (HanimeApiException e) {
            log.error("搜索视频失败: {}", e.getMessage());
            throw new BizException(ErrorCode.FAIL, "搜索视频失败: " + e.getMessage());
        } catch (HanimeNetworkException e) {
            log.error("搜索视频网络异常: {}", e.getMessage());
            throw new BizException(ErrorCode.FAIL, "网络异常，请稍后重试");
        }
        if (result != null) {
            replaceVideoUrlsInVideoInfoList(result.getVideos());
        }
        return result;
    }

    // ==================== 观看页 ====================

    /**
     * 获取观看页，替换 videoUrls 和 relatedHanimes 中的视频 URL
     */
    public WatchPage getWatchPage(String videoCode) {
        WatchPage watchPage;
        try {
            watchPage = hanimeApiClient.getWatchPage(videoCode);
        } catch (HanimeApiException e) {
            log.error("获取观看页失败 videoCode={}: {}", videoCode, e.getMessage());
            throw new BizException(ErrorCode.FAIL, "获取视频详情失败: " + e.getMessage());
        } catch (HanimeNetworkException e) {
            log.error("获取观看页网络异常 videoCode={}: {}", videoCode, e.getMessage());
            throw new BizException(ErrorCode.FAIL, "网络异常，请稍后重试");
        }
        if (watchPage != null) {
            replaceVideoUrlsInWatchPage(watchPage, videoCode);

            // 替换相关影片列表中的视频 URL
            if (!CollectionUtils.isEmpty(watchPage.getRelatedHanimes())) {
                replaceVideoUrlsInVideoInfoList(watchPage.getRelatedHanimes());
            }
        }
        return watchPage;
    }

    // ==================== 用户页 ====================

    /**
     * 获取用户页（首页 Tab），替换 videoList 和 playlists 中的相关 URL
     */
    public UserPage getUserPage(String userId) {
        UserPage userPage;
        try {
            userPage = hanimeApiClient.getUserPage(userId);
        } catch (HanimeApiException e) {
            log.error("获取用户页失败 userId={}: {}", userId, e.getMessage());
            throw new BizException(ErrorCode.FAIL, "获取用户数据失败: " + e.getMessage());
        } catch (HanimeNetworkException e) {
            log.error("获取用户页网络异常 userId={}: {}", userId, e.getMessage());
            throw new BizException(ErrorCode.FAIL, "网络异常，请稍后重试");
        }
        if (userPage != null) {
            replaceVideoUrlsInVideoInfoList(userPage.getVideoList());
            // playlists 中的 listUrl 暂不替换（无本地缓存对应关系）
        }
        return userPage;
    }

    /**
     * 获取用户上传视频列表（视频 Tab）
     */
    public VideosResult getUserVideos(CommonParam params) {
        VideosResult result;
        try {
            result = hanimeApiClient.getUploadVideos(params);
        } catch (HanimeApiException e) {
            log.error("获取用户视频列表失败 code={}: {}", params.getCode(), e.getMessage());
            throw new BizException(ErrorCode.FAIL, "获取用户视频列表失败: " + e.getMessage());
        } catch (HanimeNetworkException e) {
            log.error("获取用户视频列表网络异常 code={}: {}", params.getCode(), e.getMessage());
            throw new BizException(ErrorCode.FAIL, "网络异常，请稍后重试");
        }
        if (result != null) {
            replaceVideoUrlsInVideoInfoList(result.getVideos());
        }
        return result;
    }

    /**
     * 获取用户播放清单列表（清单 Tab）
     */
    public PlaylistsResult getUserPlaylists(CommonParam params) {
        try {
            return hanimeApiClient.getPlaylists(params);
        } catch (HanimeApiException e) {
            log.error("获取用户播放清单失败 code={}: {}", params.getCode(), e.getMessage());
            throw new BizException(ErrorCode.FAIL, "获取播放清单失败: " + e.getMessage());
        } catch (HanimeNetworkException e) {
            log.error("获取用户播放清单网络异常 code={}: {}", params.getCode(), e.getMessage());
            throw new BizException(ErrorCode.FAIL, "网络异常，请稍后重试");
        }
    }

    // ==================== 播放列表页 ====================

    /**
     * 获取播放列表详情，替换 videos 中的视频 URL
     */
    public Playlist getPlaylist(CommonParam params) {
        Playlist playlist;
        try {
            playlist = hanimeApiClient.getPlaylist(params);
        } catch (HanimeApiException e) {
            log.error("获取播放列表失败 code={}: {}", params.getCode(), e.getMessage());
            throw new BizException(ErrorCode.FAIL, "获取播放列表失败: " + e.getMessage());
        } catch (HanimeNetworkException e) {
            log.error("获取播放列表网络异常 code={}: {}", params.getCode(), e.getMessage());
            throw new BizException(ErrorCode.FAIL, "网络异常，请稍后重试");
        }
        if (playlist != null) {
            replaceVideoUrlsInVideoInfoList(playlist.getVideos());
        }
        return playlist;
    }

    // ==================== 评论 ====================

    /**
     * 获取评论列表（透传）
     */
    public List<Comment> getComments(String type, String id) {
        try {
            return hanimeApiClient.getComments(type, id);
        } catch (HanimeApiException e) {
            log.error("获取评论失败 type={} id={}: {}", type, id, e.getMessage());
            throw new BizException(ErrorCode.FAIL, "获取评论失败: " + e.getMessage());
        } catch (HanimeNetworkException e) {
            log.error("获取评论网络异常 type={} id={}: {}", type, id, e.getMessage());
            throw new BizException(ErrorCode.FAIL, "网络异常，请稍后重试");
        }
    }

    /**
     * 获取评论回复列表（透传）
     */
    public List<Comment> getReplies(String commentId) {
        try {
            return hanimeApiClient.getReplies(commentId);
        } catch (HanimeApiException e) {
            log.error("获取评论回复失败 commentId={}: {}", commentId, e.getMessage());
            throw new BizException(ErrorCode.FAIL, "获取评论回复失败: " + e.getMessage());
        } catch (HanimeNetworkException e) {
            log.error("获取评论回复网络异常 commentId={}: {}", commentId, e.getMessage());
            throw new BizException(ErrorCode.FAIL, "网络异常，请稍后重试");
        }
    }

    // ==================== URL 替换核心方法 ====================

    /**
     * 替换 WatchPage 中的 videoUrls（实际视频文件 URL）
     * 将上游 CDN 地址替换为本地 /cache/getVideo 地址
     */
    private void replaceVideoUrlsInWatchPage(WatchPage watchPage, String videoCode) {
        Map<String, VideoQuality> videoUrls = watchPage.getVideoUrls();
        if (CollectionUtils.isEmpty(videoUrls)) {
            return;
        }

        // 查询本地已缓存的视频文件（按分辨率 -> 路径）
        List<Video> cachedVideos = videoService.getVideoByCode(videoCode);
        Map<String, String> cachedPathMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(cachedVideos)) {
            for (Video v : cachedVideos) {
                if (v.getResolution() != null && v.getPath() != null) {
                    cachedPathMap.put(v.getResolution().toLowerCase(), v.getPath());
                }
            }
        }

        // 替换每个分辨率的 URL
        for (Map.Entry<String, VideoQuality> entry : videoUrls.entrySet()) {
            String resolution = entry.getKey().toLowerCase();
            VideoQuality quality = entry.getValue();

            if (cachedPathMap.containsKey(resolution)) {
                // 有本地缓存 → 替换为本地地址
                String localUrl = String.format("/cache/getVideo?videoCode=%s&resolution=%s",
                        videoCode, resolution);
                quality.setUrl(localUrl);
                log.debug("替换 WatchPage videoUrl: {} -> {}", videoCode, localUrl);
            }
            // 无缓存 → 保留上游原始 URL
        }
    }

    /**
     * 批量替换 VideoInfo 列表中的 videoUrl
     * 若本地有缓存，则将 videoUrl 替换为代理地址
     */
    private void replaceVideoUrlsInVideoInfoList(List<VideoInfo> videoInfoList) {
        if (CollectionUtils.isEmpty(videoInfoList)) {
            return;
        }

        // 批量查询已缓存的 videoCode 集合
        Set<String> videoCodes = videoInfoList.stream()
                .map(VideoInfo::getVideoCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (videoCodes.isEmpty()) {
            return;
        }

        Set<String> cachedVideoCodes = getCachedVideoCodes(videoCodes);

        if (cachedVideoCodes.isEmpty()) {
            return;
        }

        // 替换已缓存视频的 videoUrl
        for (VideoInfo videoInfo : videoInfoList) {
            String code = videoInfo.getVideoCode();
            if (code != null && cachedVideoCodes.contains(code)) {
                String proxyUrl = "/api/proxy/watch?v=" + code;
                videoInfo.setVideoUrl(proxyUrl);
                log.debug("替换 VideoInfo videoUrl: {} -> {}", code, proxyUrl);
            }
        }
    }

    /**
     * 批量查询已缓存的 videoCode 集合
     */
    private Set<String> getCachedVideoCodes(Set<String> videoCodes) {
        if (CollectionUtils.isEmpty(videoCodes)) {
            return Collections.emptySet();
        }

        List<Video> cachedVideos = videoService.lambdaQuery()
                .in(Video::getVideoCode, videoCodes)
                .isNotNull(Video::getPath)
                .select(Video::getVideoCode)
                .list();

        if (CollectionUtils.isEmpty(cachedVideos)) {
            return Collections.emptySet();
        }

        return cachedVideos.stream()
                .map(Video::getVideoCode)
                .collect(Collectors.toSet());
    }
}
