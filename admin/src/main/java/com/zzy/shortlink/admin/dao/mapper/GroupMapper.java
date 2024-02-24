package com.zzy.shortlink.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzy.shortlink.admin.dao.entity.GroupDO;
import com.zzy.shortlink.admin.dao.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMapper extends BaseMapper<GroupDO> {
}
