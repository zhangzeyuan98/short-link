package com.zzy.shortlink.project.toolkit;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.zzy.shortlink.project.common.constant.ShortLinkConstant;
import lombok.Data;

import java.util.Date;
import java.util.Optional;

public class LinkUtil {

    public static long getLinkCacheValidTime(Date validDate){
        return Optional.ofNullable(validDate)
                .map(each -> DateUtil.between(new Date(), each, DateUnit.MS))
                .orElse(ShortLinkConstant.DEFAULT_CACHE_VALID_TIME);
    }
}
