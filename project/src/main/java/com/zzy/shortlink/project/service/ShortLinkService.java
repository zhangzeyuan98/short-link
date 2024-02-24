package com.zzy.shortlink.project.service;

import cn.hutool.http.server.HttpServerRequest;
import cn.hutool.http.server.HttpServerResponse;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzy.shortlink.project.dao.entity.ShortLinkAccessStatsDTO;
import com.zzy.shortlink.project.dao.entity.ShortLinkDO;
import com.zzy.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.zzy.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.zzy.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.zzy.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.zzy.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.zzy.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import com.zzy.shortlink.project.dto.resp.ShortLinkPageRespDTO;

import java.util.List;

public interface ShortLinkService extends IService<ShortLinkDO> {
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParams);

    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParams);

    List<ShortLinkGroupCountQueryRespDTO> groupShortLinkCount(List<String> requestParams);

    void restoreUrl(String shortUri, ServletRequest request, ServletResponse response);

    ShortLinkAccessStatsDTO buildShortLinkStats(String fullShortUrl, String gid,  ServletRequest request, ServletResponse response);

    void updateShortLink(ShortLinkUpdateReqDTO requestParam);

    void shortLinkStats(String fullShortUrl, String gid, ShortLinkAccessStatsDTO statsRecord);
}
