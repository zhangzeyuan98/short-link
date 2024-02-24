package com.zzy.shortlink.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzy.shortlink.project.common.constant.RedisKeyConstant;
import com.zzy.shortlink.project.dao.entity.ShortLinkDO;
import com.zzy.shortlink.project.dao.mapper.ShortLinkMapper;
import com.zzy.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.zzy.shortlink.project.service.RecycleBinService;
import com.zzy.shortlink.project.toolkit.LinkUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RecycleBinServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO>  implements RecycleBinService {

    private final StringRedisTemplate stringRedisTemplate;
    @Override
    public void saveRecycleBin(RecycleBinSaveReqDTO requestParams) {
        LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                .eq(ShortLinkDO::getFullShortUrl, requestParams.getFullShortUrl())
                .eq(ShortLinkDO::getGid, requestParams.getGid())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0);

        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .enableStatus(1)
                .build();
        baseMapper.update(shortLinkDO, updateWrapper);
        // 将短链接移至回收站时，同时在缓存中删除该短链接
        stringRedisTemplate.delete(
                String.format(RedisKeyConstant.GOTO_SHORT_LINK_KEY, requestParams.getFullShortUrl())
        );
    }
}
