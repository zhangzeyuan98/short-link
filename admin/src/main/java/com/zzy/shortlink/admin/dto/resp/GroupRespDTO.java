package com.zzy.shortlink.admin.dto.resp;

import lombok.Data;

@Data
public class GroupRespDTO {
    /**
     * id
     */
    private Long id;

    /**
     * 分组标识
     */
    private String gid;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 分组排序
     */
    private Integer sortOrder;

    /**
     * 当前分组下有多少短链接数量
     */
    private Integer shortLinkCount;
}
