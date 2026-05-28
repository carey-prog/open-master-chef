package org.carey.masterchef.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步线程池配置。
 * <p>
 * Spring 的 {@code @Async} 注解需要指定线程池 Bean 名称，
 * {@link org.carey.masterchef.service.RecipeGenerateRunner#run} 使用 {@code @Async("recipeExecutor")}。
 * </p>
 *
 * <p>参数说明：</p>
 * <ul>
 *   <li>corePoolSize=2 — 常驻 2 个线程，适合本地开发</li>
 *   <li>maxPoolSize=4 — 高峰期最多 4 个并发菜谱生成</li>
 *   <li>queueCapacity=50 — 超过 4 个并发时，最多排队 50 个任务</li>
 * </ul>
 */
@Configuration
@EnableAsync  // 启用 @Async 注解支持
public class AsyncConfig {

    @Bean(name = "recipeExecutor")
    public Executor recipeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("recipe-gen-"); // 线程名前缀，便于在日志中识别
        executor.initialize();
        return executor;
    }
}
