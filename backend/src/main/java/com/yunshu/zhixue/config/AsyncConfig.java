package com.yunshu.zhixue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池：操作日志异步落库（02 §2.5，不引入 MQ）。
 */
@Configuration
public class AsyncConfig {

    public static final String OPERATION_LOG_EXECUTOR = "operationLogExecutor";

    @Bean(OPERATION_LOG_EXECUTOR)
    public ThreadPoolTaskExecutor operationLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("op-log-");
        // 队列满时由调用线程执行，保证日志不丢失且不影响主流程
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
