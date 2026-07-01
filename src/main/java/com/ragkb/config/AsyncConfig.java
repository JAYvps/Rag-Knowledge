// ============ config/AsyncConfig.java ============
package com.ragkb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步线程池配置
 *
 * 文档处理是IO密集型任务（调Embedding API + 写数据库）
 * 配置专用线程池，避免占用公共线程池
 */
@Configuration
public class AsyncConfig {

    @Bean("docProcessExecutor")
    public Executor docProcessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);       // 核心线程数
        executor.setMaxPoolSize(5);        // 最大线程数
        executor.setQueueCapacity(100);    // 队列容量
        executor.setThreadNamePrefix("doc-process-");
        executor.initialize();
        return executor;
    }
}
