package com.zzy.shortlink.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzy.shortlink.project.dao.entity.ShortLinkDO;
import com.zzy.shortlink.project.dto.req.RecycleBinSaveReqDTO;

public interface RecycleBinService extends IService<ShortLinkDO> {
    void saveRecycleBin(RecycleBinSaveReqDTO requestParams);
}
