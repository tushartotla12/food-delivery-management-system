package org.dmg.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration
 * Enables asynchronous processing and configures the thread pool for async operations.
 * This allows notification sending, email delivery, and other I/O operations to happen
 * without blocking the main request-response flow.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Configure the async executor for notification delivery.
     * This thread pool will be used for all @Async methods.
     *
     * Core pool size: 5 threads (minimum threads always running)
     * Max pool size: 10 threads (maximum threads allowed)
     * Queue capacity: 100 (tasks queue size before rejecting)
     * Thread name prefix: "async-notifications-"
     *
     * @return Configured ThreadPoolTaskExecutor
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);                          // Min threads
        executor.setMaxPoolSize(10);                          // Max threads
        executor.setQueueCapacity(100);                       // Queue size
        executor.setThreadNamePrefix("async-notifications-"); // Thread name prefix
        executor.setAwaitTerminationSeconds(60);              // Wait max 60 seconds before shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);   // Wait for tasks to complete
        executor.initialize();
        return executor;
    }
}

