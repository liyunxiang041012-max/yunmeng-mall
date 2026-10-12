package com.liyun.user.utils;

import jakarta.annotation.PostConstruct;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.stereotype.Component;

@Component
public class IpUtils {

    private static Searcher searcher;

    @PostConstruct
    public void init() throws Exception {
        String path = this.getClass().getResource("/ip2region_v4.xdb").getPath();
        // 整个文件缓存到内存，查询极快
        byte[] cBuff = Searcher.loadContentFromFile(path);
        searcher = Searcher.newWithBuffer(cBuff);
    }

    // 返回例如："中国|0|广东省|深圳市|电信"
    public static String getRegion(String ip) {
        try {
            if ("127.0.0.1".equals(ip) || ip.startsWith("192.168") || ip.startsWith("10.")) {
                return "内网IP";
            }
            return searcher.search(ip);
        } catch (Exception e) {
            return "未知";
        }
    }

}