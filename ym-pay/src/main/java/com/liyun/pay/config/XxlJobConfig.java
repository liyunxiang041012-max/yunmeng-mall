package com.liyun.pay.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.accessToken:}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Value("${xxl.job.executor.address:}")
    private String address;

    @PostConstruct
    public void init() {
        log.info("XXL-JOB 执行器初始化: appname={}, port={}, address={}, admin={}", appname, port, address, adminAddresses);
    }

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setPort(port);
        if (address != null && !address.isEmpty()) {
            executor.setAddress(address + ":" + port);
        }
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        if (accessToken != null && !accessToken.isEmpty()) {
            executor.setAccessToken(accessToken);
        }
        log.info("XXL-JOB 执行器 Bean 创建成功");
        return executor;
    }
}
