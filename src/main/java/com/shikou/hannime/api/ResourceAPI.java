package com.shikou.hannime.api;

import com.shikou.hannime.config.VideoStoreConfig;
import com.shikou.hannime.entities.domain.Video;
import com.shikou.hannime.service.VideoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/resource")
public class ResourceAPI {
    
    @Resource
    private VideoService videoService;
    
    @Resource
    private VideoStoreConfig videoStoreConfig;

    /**
     * 获取视频文件（支持Range请求，用于断点续传）
     * @param videoCode 视频代码
     * @param resolution 分辨率，如 "360p", "720p", "1080p"
     * @param request HTTP请求对象，用于处理Range头
     * @param response HTTP响应对象，用于设置响应头
     */
    @GetMapping("/getVideo")
    public void getVideo(
            @RequestParam String videoCode,
            @RequestParam(defaultValue = "360P") String resolution,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // 1. 参数校验
        if (!videoCode.matches("^\\d+$")) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid videoCode");
            return;
        }
        resolution = resolution.toUpperCase().trim();
        if (!resolution.matches("^(?i)(240p|360p|480p|720p|1080p)$")) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Invalid resolution");
            return;
        }

        log.debug("Streaming video: {}, resolution: {}", videoCode, resolution);

        // 2. 定位视频文件
        Path videoPath = resolveVideoPath(videoCode, resolution);
        if (videoPath == null || !Files.exists(videoPath)) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }

        File videoFile = videoPath.toFile();
        long fileSize = videoFile.length();
        String mimeType = determineMimeType(videoPath);

        // 3. 处理 Range 请求（断点续传）
        List<HttpRange> httpRanges = HttpRange.parseRanges(request.getHeader("Range"));
        if (httpRanges.isEmpty()) {
            // 完整文件响应
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(mimeType);
            response.setHeader("Accept-Ranges", "bytes");
            response.setContentLengthLong(fileSize);
            try (InputStream is = Files.newInputStream(videoPath);
                 OutputStream os = response.getOutputStream()) {
                Files.copy(videoPath, os);
            }
        } else {
            // 只支持单个 Range（多数播放器请求）
            HttpRange range = httpRanges.get(0);
            long start = range.getRangeStart(fileSize);
            long end = range.getRangeEnd(fileSize);
            long contentLength = end - start + 1;

            response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
            response.setContentType(mimeType);
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Range", String.format("bytes %d-%d/%d", start, end, fileSize));
            response.setContentLengthLong(contentLength);

            try (RandomAccessFile raf = new RandomAccessFile(videoFile, "r");
                 OutputStream os = response.getOutputStream()) {
                raf.seek(start);
                byte[] buffer = new byte[8192];
                long remaining = contentLength;
                int bytesRead;
                while (remaining > 0 && (bytesRead = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    os.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }
        }
    }

    /**
     * 解析视频文件路径
     */
    private Path resolveVideoPath(String videoCode, String resolution) {
        List<Video> videos = videoService.getVideoByCode(videoCode);
        Path basePath = Paths.get(videoStoreConfig.getVideoStorePath());

        if (CollectionUtils.isEmpty(videos)) {
            // 尝试默认命名规则
            String fileName = videoCode + "_" + resolution + ".mp4";
            Path candidate = basePath.resolve(fileName);
            if (Files.exists(candidate)) return candidate;

            // 尝试其他扩展名
            for (String ext : Arrays.asList(".mp4", ".avi", ".mkv", ".mov", ".wmv")) {
                Path alt = basePath.resolve(videoCode + "_" + resolution + ext);
                if (Files.exists(alt)) return alt;
            }
            return null;
        }

        // 数据库中优先
        Map<String, String> pathMap = videos.stream()
                .collect(Collectors.toMap(Video::getResolution, Video::getPath, (a, b) -> a));
        String relativePath = pathMap.get(resolution);
        if (StringUtils.isNotBlank(relativePath)) {
            return basePath.resolve(relativePath);
        }

        // 备用默认路径
        return basePath.resolve(videoCode).resolve(videoCode + "_" + resolution + ".mp4");
    }

    /**
     * 根据文件扩展名确定 MIME 类型
     */
    private String determineMimeType(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(".mp4")) return "video/mp4";
        if (fileName.endsWith(".avi")) return "video/x-msvideo";
        if (fileName.endsWith(".mkv")) return "video/x-matroska";
        if (fileName.endsWith(".mov")) return "video/quicktime";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
