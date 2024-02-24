package com.zzy.shortlink.project;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zzy.shortlink.project.dao.entity.ShortLinkAccessStatsDTO;
import com.zzy.shortlink.project.dao.mapper.ShortLinkAccessStatsMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class MySQLTest {

    @Autowired
    private  ShortLinkAccessStatsMapper shortLinkAccessStatsMapper;

    @Test
    public void testUniqueIndex() {
        long pv = 1, uv = 1, uip = 1;
        long start = System.currentTimeMillis();
        for(int i = 0; i < 100000; i++) {
            ShortLinkAccessStatsDTO linkAccessStatsDTO = ShortLinkAccessStatsDTO.builder()
                    .fullShortUrl("nurl.ink/3Urr4X")
                    .pv(pv + i)
                    .hour(DateUtil.hour(new Date(), Boolean.TRUE))
                    .date(new Date())
                    .uv(uv + i)
                    .uip(uip + i)
                    .gid("8evmp6")
                    .weekday(DateUtil.dayOfWeek(new Date()))
                    .build();
            shortLinkAccessStatsMapper.shortLinkStats(linkAccessStatsDTO);
        }
        long end = System.currentTimeMillis();

        System.out.println(end - start);
        // 22750
        // 187234
    }

    @Test
    public void testNormalIndex() {
        long pv = 1, uv = 1, uip = 1;
        long start = System.currentTimeMillis();
        for(int i = 0; i < 100000; i++) {
            ShortLinkAccessStatsDTO linkAccessStatsDTO = ShortLinkAccessStatsDTO.builder()
                    .pv(pv + i)
                    .uv(uv + i)
                    .uip(uip + i)
                    .build();
//            shortLinkAccessStatsMapper.insert(linkAccessStatsDTO);
            LambdaUpdateWrapper<ShortLinkAccessStatsDTO> lambdaUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkAccessStatsDTO.class)
                    .eq(ShortLinkAccessStatsDTO::getFullShortUrl, "nurl.ink/3Urr4X")
                    .eq(ShortLinkAccessStatsDTO::getGid, "8evmp6")
                    .eq(ShortLinkAccessStatsDTO::getDate, DateUtil.format(DateUtil.date(), "yyyy-MM-dd"))
                    .eq(ShortLinkAccessStatsDTO::getHour, 22);

            shortLinkAccessStatsMapper.update(linkAccessStatsDTO, lambdaUpdateWrapper);

        }
        long end = System.currentTimeMillis();

        System.out.println(end - start);
        // 22901
        // 197510
    }

    private static final int MAX_CONCURRENCY= 8;

    private static final CountDownLatch COUNT_DOWN_LATCH = new CountDownLatch(1);
    @Test
    public void testMoreShortLinkStats() throws InterruptedException {
        for (int i = 0; i < MAX_CONCURRENCY; i++) {
            new Thread(() -> {
                try {
                    COUNT_DOWN_LATCH.await();
                    //执行业务逻辑
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        COUNT_DOWN_LATCH.countDown();
        TimeUnit.SECONDS.sleep(100);
    }
}
