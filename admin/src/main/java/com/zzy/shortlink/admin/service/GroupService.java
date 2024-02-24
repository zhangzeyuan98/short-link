package com.zzy.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzy.shortlink.admin.dao.entity.GroupDO;
import com.zzy.shortlink.admin.dto.req.GroupSortReqDTO;
import com.zzy.shortlink.admin.dto.req.GroupUpdateReqDTO;
import com.zzy.shortlink.admin.dto.resp.GroupRespDTO;

import java.util.List;

public interface GroupService extends IService<GroupDO> {
    void saveGroup(String groupName);

    void saveGroup(String username, String groupName);

    List<GroupRespDTO> listGroup();

    void updateGroup(GroupUpdateReqDTO requestParams);

    void deleteGroup(String gid);

    void sortGroup(List<GroupSortReqDTO> requestParams);
}
