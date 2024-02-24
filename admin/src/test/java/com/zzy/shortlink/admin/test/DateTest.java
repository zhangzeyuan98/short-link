package com.zzy.shortlink.admin.test;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

public class DateTest {

    public static long computeValidTimeOnCache() {
        return 1111111;
    }

    public static long getLinkCacheValidTime(Date validDate, int validType){
        return Optional.ofNullable(validDate)
                .map(each -> {
                    Date date = new Date();
                    long computeValidTimeOnCache = computeValidTimeOnCache();
                    long betweenDate = DateUtil.between(new Date(), each, DateUnit.MS);
                    if(validType == 1) {
                        return computeValidTimeOnCache;
                    } else {
                        if(validDate.after(date)) {
                            if(date.getTime() + computeValidTimeOnCache > validDate.getTime()) {
                                return betweenDate;
                            } else {
                                return computeValidTimeOnCache;
                            }
                        } else {
                            return -1L;
                        }
                    }
                })
                .orElse(Long.valueOf("30000000000000"));
    }

    public static void main(String[] args) {
        String dateTimeString = "2024-02-17 21:35:12";

        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 将字符串解析为LocalDateTime
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, formatter);

        // 如果需要，将LocalDateTime转换为Date
        Date validDate = Date.from(localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());

        long linkCacheValidTime = getLinkCacheValidTime(validDate, 0);
//        System.out.println(linkCacheValidTime);
        System.out.println(DateUtil.format(DateUtil.date(), "yyyyMMdd"));
    }
}
