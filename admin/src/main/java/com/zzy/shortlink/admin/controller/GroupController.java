package com.zzy.shortlink.admin.controller;

import com.zzy.shortlink.admin.common.convention.result.Result;
import com.zzy.shortlink.admin.common.convention.result.Results;
import com.zzy.shortlink.admin.dto.req.GroupSaveReqDTO;
import com.zzy.shortlink.admin.dto.req.GroupSortReqDTO;
import com.zzy.shortlink.admin.dto.req.GroupUpdateReqDTO;
import com.zzy.shortlink.admin.dto.resp.GroupRespDTO;
import com.zzy.shortlink.admin.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/api/short-link/v1/group")
    public Result<Void> save(@RequestBody GroupSaveReqDTO requestParams) {
        groupService.saveGroup(requestParams.getName());
        return Results.success();
    }

    @GetMapping("/api/short-link/v1/group")
    public Result<List<GroupRespDTO>> listGroup() {
        return Results.success(groupService.listGroup());
    }

    @PutMapping("/api/short-link/v1/group")
    public Result<Void> updateGroup(@RequestBody GroupUpdateReqDTO requestParams) {
        groupService.updateGroup(requestParams);
        return Results.success();
    }

    @DeleteMapping("/api/short-link/v1/group")
    public Result<Void> deleteGroup(@RequestParam String gid) {
        groupService.deleteGroup(gid);
        return Results.success();
    }

    @PostMapping("/api/short-link/v1/group/sort")
    public Result<Void> sortGroup(@RequestBody List<GroupSortReqDTO> requestParams) {
        groupService.sortGroup(requestParams);
        return Results.success();
    }
}
