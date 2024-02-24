package com.zzy.shortlink.admin.remote;


import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zzy.shortlink.admin.common.convention.result.Result;
import com.zzy.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.zzy.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.zzy.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.zzy.shortlink.admin.remote.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.zzy.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ShortLinkRemoteService {
    default Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParams) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("gid", requestParams.getGid());
        requestMap.put("current", requestParams.getCurrent());
        requestMap.put("size", requestParams.getSize());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/page", requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>(){});
    }

    default Result<ShortLinkCreateRespDTO> createShortLink(ShortLinkCreateReqDTO requestParams) {
        String resultBody = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/create", JSON.toJSONString(requestParams));
        return JSON.parseObject(resultBody, new TypeReference<>(){});
    }

    default Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(List<String> requestParams) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("requestParams", requestParams);
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/count", requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>(){});
    }
}
