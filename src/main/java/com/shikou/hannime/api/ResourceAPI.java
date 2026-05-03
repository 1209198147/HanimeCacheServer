package com.shikou.hannime.api;

import com.shikou.hannime.config.VideoStoreConfig;
import com.shikou.hannime.entities.domain.Video;
import com.shikou.hannime.service.VideoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/resources")
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
    @GetMapping("/video")
    public void getVideo(
            @RequestParam("videoCode") String videoCode,
            @RequestParam(value = "resolution", defaultValue = "360p") String resolution,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        // 根据videoCode和resolution查询视频信息
        List<Video> videos = videoService.getVideoByCode(videoCode);
        
        String filePath;
        if (videos == null) {
            // 如果数据库中没有找到，尝试根据默认路径构建文件路径
            String fileName = videoCode + "_" + resolution + ".mp4";
            filePath = Paths.get(videoStoreConfig.getVideoStorePath(), fileName).toString();
            
            File file = new File(filePath);
            if (!file.exists()) {
                // 尝试其他常见扩展名
                String[] extensions = {".mp4", ".avi", ".mkv", ".mov", ".wmv"};
                for (String ext : extensions) {
                    String altFilePath = Paths.get(videoStoreConfig.getVideoStorePath(), videoCode + "_" + resolution + ext).toString();
                    File altFile = new File(altFilePath);
                    if (altFile.exists()) {
                        file = altFile;
                        filePath = altFilePath;
                        break;
                    }
                }
            }
            
            if (!new File(filePath).exists()) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return;
            }
        } else {
            // 如果数据库中有记录，使用path字段的值
            Map<String, String> resolutionPathMap = videos.stream().collect(Collectors.toMap(Video::getResolution, Video::getPath));
            String resolutionPath = resolutionPathMap.get(resolution);
            if (!StringUtils.isBlank(resolutionPath)) {
                // 如果path为空，构建默认路径
                String fileName = videoCode + "_" + resolution + ".mp4";
                filePath = Paths.get(videoStoreConfig.getVideoStorePath(), fileName).toString();
            }else{
                filePath = Paths.get(videoStoreConfig.getVideoStorePath(), resolutionPath).toString();
            }
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        
        long fileLength = file.length();
        String rangeHeader = request.getHeader("Range");
        
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            // 处理Range请求
            try {
                String[] ranges = rangeHeader.substring(6).split("-");
                long start = Long.parseLong(ranges[0].trim());
                long end = fileLength - 1;
                
                if (ranges.length > 1 && !ranges[1].trim().isEmpty()) {
                    end = Long.parseLong(ranges[1].trim());
                }
                
                if (start < 0 || start >= fileLength || end < start) {
                    response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
                    response.setHeader("Content-Range", "bytes */" + fileLength);
                    return;
                }
                
                if (end >= fileLength) {
                    end = fileLength - 1;
                }
                
                long contentLength = end - start + 1;
                
                response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
                response.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
                response.setHeader("Accept-Ranges", "bytes");
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                response.setHeader("Content-Length", String.valueOf(contentLength));
                
                // 设置Content-Disposition为inline以支持浏览器内嵌播放
                response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
                
                // 流式传输文件
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                     OutputStream outputStream = response.getOutputStream()) {
                    
                    randomAccessFile.seek(start);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long bytesRemaining = contentLength;
                    
                    while (bytesRemaining > 0 && (bytesRead = randomAccessFile.read(buffer, 0, 
                            (int) Math.min(buffer.length, bytesRemaining))) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        bytesRemaining -= bytesRead;
                    }
                }
                
            } catch (Exception e) {
                response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
                response.setHeader("Content-Range", "bytes */" + fileLength);
                return;
            }
        } else {
            // 普通请求，返回整个文件
            response.setStatus(HttpStatus.OK.value());
            response.setHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Length", String.valueOf(fileLength));
            response.setHeader("Content-Range", "bytes 0-" + (fileLength - 1) + "/" + fileLength);
            
            // 设置Content-Disposition为inline以支持浏览器内嵌播放
            response.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
            
            // 流式传输文件
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream outputStream = response.getOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
