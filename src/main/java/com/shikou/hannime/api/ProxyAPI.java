package com.shikou.hannime.api;

import com.shikou.hannime.entities.response.Result;
import com.shikou.hannime.service.ProxyService;
import com.shikou.model.entities.CommonParam;
import com.shikou.model.entities.SearchParams;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代理转发 API —— 接收前端请求，通过 HanimeClient 调用上游，
 * 将响应中的视频 URL 替换为本地缓存地址后返回给前端
 */
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/proxy")
public class ProxyAPI {

    @Resource
    private ProxyService proxyService;

    // ==================== 首页 ====================

    /**
     * GET /api/proxy/home — 首页
     */
    @GetMapping("/home")
    public Result home() {
        return Result.success(proxyService.getHomePage());
    }

    // ==================== 搜索页 ====================

    /**
     * GET /api/proxy/search — 搜索页（首次，含筛选条件）
     */
    @GetMapping("/search")
    public Result search(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String duration,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page) {

        SearchParams params = SearchParams.builder()
                .genre(genre)
                .tags(tags)
                .sort(sort)
                .date(date)
                .duration(duration)
                .query(query)
                .page(page)
                .build();

        return Result.success(proxyService.getSearchPage(params));
    }

    /**
     * GET /api/proxy/search/videos — 搜索视频（翻页/后续搜索，仅视频列表）
     */
    @GetMapping("/search/videos")
    public Result searchVideos(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String duration,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page) {

        SearchParams params = SearchParams.builder()
                .genre(genre)
                .tags(tags)
                .sort(sort)
                .date(date)
                .duration(duration)
                .query(query)
                .page(page)
                .build();

        return Result.success(proxyService.search(params));
    }

    // ==================== 观看页 ====================

    /**
     * GET /api/proxy/watch?v={code} — 观看页
     */
    @GetMapping("/watch")
    public Result watch(@RequestParam("v") String v) {
        return Result.success(proxyService.getWatchPage(v));
    }

    // ==================== 用户页 ====================

    /**
     * GET /api/proxy/user/{userId} — 用户页（首页 Tab）
     */
    @GetMapping("/user/{userId}")
    public Result user(@PathVariable String userId) {
        return Result.success(proxyService.getUserPage(userId));
    }

    /**
     * GET /api/proxy/user/{userId}/videos — 用户页（视频 Tab）
     */
    @GetMapping("/user/{userId}/videos")
    public Result userVideos(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String sort) {

        CommonParam params = CommonParam.builder()
                .code(userId)
                .page(page)
                .sort(sort)
                .build();

        return Result.success(proxyService.getUserVideos(params));
    }

    /**
     * GET /api/proxy/user/{userId}/playlists — 用户页（播放清单 Tab）
     */
    @GetMapping("/user/{userId}/playlists")
    public Result userPlaylists(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String sort) {

        CommonParam params = CommonParam.builder()
                .code(userId)
                .page(page)
                .sort(sort)
                .build();

        return Result.success(proxyService.getUserPlaylists(params));
    }

    // ==================== 播放列表页 ====================

    /**
     * GET /api/proxy/playlist?list={code}&page={n} — 播放列表详情页
     */
    @GetMapping("/playlist")
    public Result playlist(
            @RequestParam("list") String list,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String sort) {

        CommonParam params = CommonParam.builder()
                .code(list)
                .page(page)
                .sort(sort)
                .build();

        return Result.success(proxyService.getPlaylist(params));
    }

    // ==================== 评论 ====================

    /**
     * GET /api/proxy/comment?type=video&id={code} — 评论列表
     */
    @GetMapping("/comment")
    public Result comments(
            @RequestParam String type,
            @RequestParam String id) {

        return Result.success(proxyService.getComments(type, id));
    }

    /**
     * GET /api/proxy/comment/replies?id={id} — 评论回复
     */
    @GetMapping("/comment/replies")
    public Result replies(@RequestParam String id) {
        return Result.success(proxyService.getReplies(id));
    }
}
