package com.zzy.shortlink.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zzy.shortlink.admin.common.convention.result.Result;
import com.zzy.shortlink.admin.remote.ShortLinkRemoteService;
import com.zzy.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.zzy.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.zzy.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.zzy.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShortLinkController {

    /**
     *  后续重构为Spring Cloud Feign 调用
     */
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {};
    @PostMapping("/api/short-link/admin/v1/create")
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO requestParams) {
        return shortLinkRemoteService.createShortLink(requestParams);
    }

    @GetMapping("/api/short-link/admin/v1/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParams) {
        return shortLinkRemoteService.pageShortLink(requestParams);
    }
}
