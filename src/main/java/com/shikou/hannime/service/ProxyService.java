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
        }
        return watchPage;
    }

    // ==================== 用户页 ====================

    /**
     * 获取用户页（首页 Tab）
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
     * 获取播放列表详情
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
     * 替换 WatchPage 中的 videoUrls（实际视频文件 URL）及 relatedHanimes 中的视频链接
     * - 视频源 URL: 将上游 CDN 地址替换为本地 /resources/ 地址
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
                if (v.getQuality() != null && v.getPath() != null) {
                    cachedPathMap.put(v.getQuality().toUpperCase(), v.getPath());
                }
            }
        }

        // 替换每个画质的 URL
        for (Map.Entry<String, VideoQuality> entry : videoUrls.entrySet()) {
            String quality = entry.getKey().toUpperCase();
            VideoQuality videoQuality = entry.getValue();

            if (cachedPathMap.containsKey(quality)) {
                // 有本地缓存 → 替换为本地地址
                String localUrl = "/resources/" + cachedPathMap.get(quality);
                videoQuality.setUrl(localUrl);
                log.debug("替换 WatchPage videoUrl: {} -> {}", videoCode, localUrl);
            }
            // 无缓存 → 保留上游原始 URL
        }
    }
}
