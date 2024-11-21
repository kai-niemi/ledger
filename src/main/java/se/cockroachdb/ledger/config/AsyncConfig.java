package se.cockroachdb.ledger.config;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${application.task-executor.concurrency-limit}")
    private int concurrencyLimit;

    @Override
    public AsyncTaskExecutor getAsyncExecutor() {
        return workloadTaskExecutor();
    }

    @Bean(name = "workloadTaskExecutor")
    public SimpleAsyncTaskExecutor workloadTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setThreadNamePrefix("workload-");
        executor.setConcurrencyLimit(concurrencyLimit);
        executor.setVirtualThreads(true);
        return executor;
    }

    @Bean("applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster(
            @Autowired @Qualifier("workloadTaskExecutor") Executor executor) {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(executor);
        eventMulticaster.setErrorHandler(t -> {
            logger.error("Unexpected error occurred in scheduled task", t);
        });
        return eventMulticaster;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            logger.error("Unexpected exception occurred invoking async method: " + method, ex);
        };
    }

    @Bean
    public CallableProcessingInterceptor callableProcessingInterceptor() {
        return new TimeoutCallableProcessingInterceptor();
    }
}


