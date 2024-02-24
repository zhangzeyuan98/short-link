package com.zzy.shortlink.admin.dto.req;

import lombok.Data;

@Data
public class GroupSortReqDTO {
    /**
     * 分组名称
     */
    private String gid;

    /**
     * 分组排序
     */
    private Integer sortOrder;
}
