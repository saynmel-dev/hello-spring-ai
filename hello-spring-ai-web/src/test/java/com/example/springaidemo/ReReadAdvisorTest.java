package com.example.springaidemo;

import com.example.springaidemo.advisor.ReReadAdvisor;
import com.example.springaidemo.advisor.LoggingAdvisor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReReadAdvisor (RE2 重读策略) 测试。
 * <p>
 * 演示场景：
 * 1. 基本重读增强 —— 验证模型能正常响应
 * 2. 逻辑推理题 —— RE2 策略的典型应用场景
 * 3. 自定义中文重读前缀
 * 4. 与其他 Advisor 组合使用
 * 5. 流式调用中的重读增强
 */
@SpringBootTest
class ReReadAdvisorTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    /**
     * 场景一：基本重读增强，验证 Advisor 正常工作
     */
    @Test
    void testBasicReRead() {
        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
                .user("什么是 Spring AI？用一句话回答。")
                .advisors(new ReReadAdvisor())
                .call()
                .content();

        System.out.println("回复: " + response);
        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 场景二：逻辑推理题 —— RE2 重读策略的典型应用场景
     * <p>
     * 重读可以帮助模型更仔细地审题，减少因粗心导致的推理错误。
     */
    @Test
    void testReReadWithReasoningQuestion() {
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new ReReadAdvisor())
                .build();

        String response = chatClient.prompt()
                .user("一个房间里有3盏灯，房间外有3个开关分别控制这3盏灯。" +
                        "你在房间外，只能进入房间一次。如何确定每个开关控制哪盏灯？" +
                        "请简要说明方法。")
                .call()
                .content();

        System.out.println("推理题回复: " + response);
        assertNotNull(response);
        assertFalse(response.isBlank());
        // 正确答案通常涉及"先开一段时间再关，利用灯泡温度判断"
        System.out.println("回复长度: " + response.length() + " 字符");
    }

    /**
     * 场景三：自定义中文重读前缀
     */
    @Test
    void testCustomChinesePrefix() {
        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
                .user("3 + 5 × 2 = ?")
                .advisors(new ReReadAdvisor("请重新阅读以上问题：", 0))
                .call()
                .content();

        System.out.println("自定义前缀回复: " + response);
        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 场景四：与 LoggingAdvisor 组合使用
     * <p>
     * 验证多个 Advisor 可以正常协作。
     * LoggingAdvisor 会记录增强后的请求日志，可以在控制台看到重读指令。
     */
    @Test
    void testCombineWithLoggingAdvisor() {
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(
                        new ReReadAdvisor(1),       // 先执行重读增强
                        new LoggingAdvisor(2)       // 再记录日志（可以看到增强后的消息）
                )
                .build();

        String response = chatClient.prompt()
                .user("Java 中 HashMap 和 TreeMap 的区别是什么？用两句话回答。")
                .call()
                .content();

        System.out.println("组合 Advisor 回复: " + response);
        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 场景五：流式调用中的重读增强
     */
    @Test
    void testReReadWithStream() {
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new ReReadAdvisor())
                .build();

        AtomicInteger chunkCount = new AtomicInteger(0);
        StringBuilder fullResponse = new StringBuilder();

        Flux<String> stream = chatClient.prompt()
                .user("为什么 1 + 1 = 2？用一句话解释。")
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
