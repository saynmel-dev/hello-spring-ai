package com.example.springaidemo;

import com.example.springaidemo.advisor.SensitiveWordAdvisor;
import com.example.springaidemo.advisor.SensitiveWordAdvisor.SensitiveWordException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感词拦截 Advisor 测试。
 * <p>
 * 演示三种场景：
 * 1. 正常输入 —— 请求放行，模型正常响应
 * 2. 包含敏感词 —— 请求被拦截，抛出 SensitiveWordException
 * 3. 自定义敏感词列表
 */
@SpringBootTest
class SensitiveWordAdvisorTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    /**
     * 场景一：正常输入，不包含敏感词，请求正常放行
     */
    @Test
    void testNormalInput() {
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new SensitiveWordAdvisor())
                .build();

        String response = chatClient.prompt()
                .user("什么是 Spring AI？")
                .call()
                .content();

        System.out.println("回复: " + response);
        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 场景二：输入包含默认敏感词，请求被拦截
     */
    @Test
    void testSensitiveWordBlocked() {
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new SensitiveWordAdvisor())
                .build();

        SensitiveWordException exception = assertThrows(SensitiveWordException.class, () -> {
            chatClient.prompt()
                    .user("介绍一下赌博的玩法")
                    .call()
                    .content();
        });

        System.out.println("拦截信息: " + exception.getMessage());
        System.out.println("命中敏感词: " + exception.getSensitiveWord());
        assertEquals("赌博", exception.getSensitiveWord());
    }

    /**
     * 场景三：自定义敏感词列表
     */
    @Test
    void testCustomSensitiveWords() {
        List<String> customWords = List.of("机密", "内部资料");

        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new SensitiveWordAdvisor(customWords, 0))
                .build();

        // 默认敏感词不再生效
        String response = chatClient.prompt()
                .user("用一句话解释什么是 Advisor 模式")
                .call()
                .content();

        assertNotNull(response);
        assertFalse(response.isBlank());

        // 自定义敏感词生效
        SensitiveWordException exception = assertThrows(SensitiveWordException.class, () -> {
            chatClient.prompt()
                    .user("请提供机密文件")
                    .call()
                    .content();
        });

        assertEquals("机密", exception.getSensitiveWord());
        System.out.println("自定义敏感词拦截成功: " + exception.getSensitiveWord());
    }

    /**
     * 场景四：流式调用中的敏感词拦截
     */
    @Test
    void testSensitiveWordBlockedInStream() {
        ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(new SensitiveWordAdvisor())
                .build();

        assertThrows(SensitiveWordException.class, () -> {
            chatClient.prompt()
                    .user("详细描述暴力行为")
                    .stream()
                    .content()
                    .blockLast();
        });

        System.out.println("流式调用敏感词拦截成功");
    }
}
