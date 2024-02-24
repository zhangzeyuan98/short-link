package com.zzy.shortlink.project.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zzy.shortlink.project.common.database.BaseDO;
import lombok.*;

import java.util.Date;

@Data
@TableName("t_link")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortLinkDO extends BaseDO {
    /**
     * id
     */
    private Long id;

    /**
     * 域名
     */
    private String domain;

    /**
     * 短链接
     */
    private String shortUri;

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 点击量
     */
    private Integer clickNum;

    /**
     * 分组id
     */
    private String gid;

    /**
     * 启动标识 0，未启动 1
     */
    private Integer enableStatus;

    /**
     * 创建类型（控制台，接口）
     */
    private Integer createType;

    /**
     * 有效期类型（临时，永久）
     */
    private Integer validDateType;

    /**
     * 有效期
     */
    private Date validDate;

    /**
     * 相关描述
     */
    @TableField("`describe`")
    private String describe;
}
