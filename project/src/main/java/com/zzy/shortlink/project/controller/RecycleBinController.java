package com.zzy.shortlink.project.controller;

import com.zzy.shortlink.project.common.convention.result.Result;
import com.zzy.shortlink.project.common.convention.result.Results;
import com.zzy.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.zzy.shortlink.project.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecycleBinController {

    private final RecycleBinService recycleBinService;

    @PostMapping("/api/short-link/v1/recycle-bin/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO requestParams) {
        recycleBinService.saveRecycleBin(requestParams);
        return Results.success();
    }
}
