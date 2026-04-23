package com.example.springaidemo;

import com.example.springaidemo.advisor.LoggingAdvisor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoggingAdvisor 对话日志拦截测试。
 * <p>
 * 演示三种使用方式：
 * 1. 单次调用级别挂载 Advisor
 * 2. 通过 Builder 设置默认 Advisor（全局生效）
 * 3. 流式调用中的日志拦截
 */
@SpringBootTest
class LoggingAdvisorTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    /**
     * 方式一：在单次 prompt 调用时挂载 LoggingAdvisor
     */
    @Test
    void testLoggingAdvisorOnSingleCall() {
        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
                .system("你是一个简洁的助手，回答不超过两句话。")
                .user("什么是 Spring AI？")
                .advisors(new LoggingAdvisor())
                .call()
                .content();

        System.out.println("回复: " + response);
        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 方式二：通过 Builder 设置默认 Advisor，所有调用自动生效
     */
    @Test
    void testLoggingAdvisorAsDefault() {
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new LoggingAdvisor())
                .build();

        String response = chatClient.prompt()
                .user("用一句话解释什么是 Advisor 模式")
                .call()
                .content();

        System.out.println("回复: " + response);
        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 方式三：流式调用 + LoggingAdvisor
     */
    @Test
    void testLoggingAdvisorWithStream() {
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new LoggingAdvisor())
                .build();

        AtomicInteger chunkCount = new AtomicInteger(0);
        StringBuilder fullResponse = new StringBuilder();

        Flux<String> stream = chatClient.prompt()
                .user("用三句话介绍 Spring AI 的 Advisor 机制")
                .stream()
                .content();

        stream.doOnNext(chunk -> {
            System.out.print(chunk);
            fullResponse.append(chunk);
            chunkCount.incrementAndGet();
        }).blockLast();

        System.out.println("\n\n共收到 " + chunkCount.get() + " 个 chunk");
        assertFalse(fullResponse.isEmpty());
        assertTrue(chunkCount.get() > 1);
    }
}
