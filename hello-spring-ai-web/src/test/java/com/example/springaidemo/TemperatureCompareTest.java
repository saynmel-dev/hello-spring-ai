package com.example.springaidemo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 针对同一段 prompt，使用不同 temperature 配置，观察模型响应差异。
 *
 * temperature 越低（接近 0），输出越确定、越稳定；
 * temperature 越高（接近 2），输出越随机、越有创造性。
 */
@SpringBootTest
class TemperatureCompareTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private static final String PROMPT = "用一句话解释什么是人工智能";

    @Test
    void compareTemperatureResponses() {
        ChatClient chatClient = chatClientBuilder.build();

        double[] temperatures = {0.0, 0.5, 1.0, 1.5};

        for (double temp : temperatures) {
            System.out.println("========== temperature = " + temp + " ==========");

            // 同一 temperature 调用两次，观察稳定性
            String response1 = chat(chatClient, PROMPT, temp);
            String response2 = chat(chatClient, PROMPT, temp);

            System.out.println("第1次: " + response1);
            System.out.println("第2次: " + response2);
            System.out.println();

            assertNotNull(response1);
            assertNotNull(response2);
            assertFalse(response1.isBlank());
            assertFalse(response2.isBlank());
        }
    }

    @Test
    void lowTemperatureShouldBeMoreDeterministic() {
        ChatClient chatClient = chatClientBuilder.build();

        // temperature=0 时，多次调用结果应高度一致
        String r1 = chat(chatClient, PROMPT, 0.0);
        String r2 = chat(chatClient, PROMPT, 0.0);
        String r3 = chat(chatClient, PROMPT, 0.0);

        System.out.println("temperature=0 第1次: " + r1);
        System.out.println("temperature=0 第2次: " + r2);
        System.out.println("temperature=0 第3次: " + r3);

        assertNotNull(r1);
        assertNotNull(r2);
        assertNotNull(r3);

        // temperature=0 理论上每次输出应完全相同
        assertEquals(r1, r2, "temperature=0 时两次响应应一致");
        assertEquals(r2, r3, "temperature=0 时两次响应应一致");
    }

    @Test
    void highTemperatureShouldProduceVariation() {
        ChatClient chatClient = chatClientBuilder.build();

        // temperature=1.5 时，多次调用结果应有差异
        String r1 = chat(chatClient, PROMPT, 1.5);
        String r2 = chat(chatClient, PROMPT, 1.5);
        String r3 = chat(chatClient, PROMPT, 1.5);

        System.out.println("temperature=1.5 第1次: " + r1);
        System.out.println("temperature=1.5 第2次: " + r2);
        System.out.println("temperature=1.5 第3次: " + r3);

        assertNotNull(r1);
        assertNotNull(r2);
        assertNotNull(r3);

        // 高 temperature 下三次结果不太可能完全相同
        boolean allSame = r1.equals(r2) && r2.equals(r3);
        assertFalse(allSame, "temperature=1.5 时三次响应不应完全相同");
    }

    private String chat(ChatClient chatClient, String prompt, double temperature) {
        return chatClient.prompt()
                .user(prompt)
                .options(OpenAiChatOptions.builder()
                        .temperature(temperature)
                        .build())
                .call()
                .content();
    }
}
