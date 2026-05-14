package com.shikou.hannime.api;

import com.shikou.hannime.entities.request.UpdateConfigRequest;
import com.shikou.hannime.entities.response.Result;
import com.shikou.hannime.service.TaskConfigService;
import com.shikou.hannime.util.AssertUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 任务配置管理接口
 */
@RestController
@RequestMapping("/config")
public class ConfigAPI {

    @Resource
    private TaskConfigService taskConfigService;

    /**
     * 获取当前所有配置
     */
    @GetMapping
    public Result getConfig() {
        Map<String, Object> config = taskConfigService.getAllConfig();
        return Result.success(config);
    }

    /**
     * 更新配置（只更新传入的非空字段）
     */
    @PutMapping
    public Result updateConfig(@RequestBody UpdateConfigRequest request) {
        AssertUtils.nonNull(request, "请求参数不能为空");
        taskConfigService.updateConfig(
                request.getVideoStorePath(),
                request.getQuality()
        );
        Map<String, Object> config = taskConfigService.getAllConfig();
        return Result.success("配置更新成功", config);
    }
}
