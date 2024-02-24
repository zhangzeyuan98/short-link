package com.zzy.shortlink.project.service.impl;

import com.zzy.shortlink.project.service.UrlTitleService;
import org.springframework.stereotype.Service;

@Service
public class UrlTitleServiceImpl implements UrlTitleService {

    @Override
    public String getTitleByUrl(String url) {
        // TODO 导入Jsoup库发起请求，并从结果中拿到网站标题，直接返回即可
        return "";
    }
}
