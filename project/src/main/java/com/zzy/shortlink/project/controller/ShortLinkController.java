package com.zzy.shortlink.project.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zzy.shortlink.project.common.convention.result.Result;
import com.zzy.shortlink.project.common.convention.result.Results;
import com.zzy.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.zzy.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.zzy.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.zzy.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.zzy.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.zzy.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.zzy.shortlink.project.service.ShortLinkService;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShortLinkController {
    private final ShortLinkService shortLinkService;

    @GetMapping("/{short-uri}")
    public void restoreUrl(@PathVariable("short-uri") String shortUri, ServletRequest request, ServletResponse httpResponse) {
        shortLinkService.restoreUrl(shortUri, request, httpResponse);
    }

    /**
     * 创建短链接
     */
    @PostMapping("/api/short-link/v1/create")
    @SentinelResource(
            value = "create_short-link",
            blockHandler = "createShortLinkBlockHandlerMethod",
            blockHandlerClass = CustomBlockHandler.class
    )
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParams) {
        return Results.success(shortLinkService.createShortLink(requestParams));
    }

    @GetMapping("/api/short-link/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParams) {
        return Results.success(shortLinkService.pageShortLink(requestParams));
    }

    @GetMapping("/api/short-link/v1/count")
    public Result<List<ShortLinkGroupCountQueryRespDTO>> groupShortLinkCount(@RequestParam("requestParams") List<String> requestParams) {
        return Results.success(shortLinkService.groupShortLinkCount(requestParams));
    }

    /**
     * 修改短链接
     */
    @PostMapping("/api/short-link/v1/update")
    public Result<Void> updateShortLink(@RequestBody ShortLinkUpdateReqDTO requestParam) {
        shortLinkService.updateShortLink(requestParam);
        return Results.success();
    }
}
