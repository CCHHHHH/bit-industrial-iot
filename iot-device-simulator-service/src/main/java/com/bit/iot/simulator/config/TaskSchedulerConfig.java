package com.bit.iot.simulator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
/**
 * 模拟任务调度相关配置。
 */
public class TaskSchedulerConfig {

    /**
     * 创建模拟任务线程池调度器。
     *
     * @param poolSize 线程池大小
     * @return 线程池调度器
     */
    @Bean
    public ThreadPoolTaskScheduler simulatorTaskScheduler(
            @Value("${simulator.scheduler.pool-size:4}") int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("simulator-task-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    /**
     * 创建 HTTP 客户端。
     *
     * @param connectTimeoutSeconds 连接超时时间，单位秒
     * @return HTTP 客户端
     */
    @Bean
    public HttpClient simulatorHttpClient(
            @Value("${simulator.http.connect-timeout-seconds:5}") int connectTimeoutSeconds) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();
    }

    /**
     * 创建 RestTemplate 实例。
     *
     * @return RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
