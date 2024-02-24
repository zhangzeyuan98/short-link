package com.zzy.shortlink.admin.remote.dto.resp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShortLinkCreateRespDTO {

    /**
     * 分组信息
     */
    private String gid;

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 短链接
     */
    private String fullShortUrl;
}
