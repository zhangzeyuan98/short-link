package com.zzy.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzy.shortlink.project.dao.entity.ShortLinkAccessStatsDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface ShortLinkAccessStatsMapper extends BaseMapper<ShortLinkAccessStatsDTO> {

    @Insert("INSERT INTO t_link_access_stats (full_short_url, gid, date, pv, uv, uip, hour, weekday, create_time, update_time, del_flag)" +
            "VALUES(#{ShortLinkAccessStats.fullShortUrl}, #{ShortLinkAccessStats.gid}, #{ShortLinkAccessStats.date}, #{ShortLinkAccessStats.pv}, #{ShortLinkAccessStats.uv}, #{ShortLinkAccessStats.uip}, #{ShortLinkAccessStats.hour}, #{ShortLinkAccessStats.weekday}, NOW(), NOW() ,0) ON DUPLICATE KEY UPDATE" +
            " pv = #{ShortLinkAccessStats.pv}, uv = #{ShortLinkAccessStats.uv}, uip = #{ShortLinkAccessStats.uip};")
    void shortLinkStats(@Param("ShortLinkAccessStats") ShortLinkAccessStatsDTO shortLinkAccessStatsDTO);
}
