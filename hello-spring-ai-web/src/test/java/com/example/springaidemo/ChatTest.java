package com.example.springaidemo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ChatTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void simpleChatTest() {
        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
                .user("你好，请用一句话介绍你自己")
                .call()
                .content();

        System.out.println("DeepSeek 回复: " + response);

        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    @Test
    void streamChatTest() {
        ChatClient chatClient = chatClientBuilder.build();
        AtomicInteger chunkCount = new AtomicInteger(0);

        Flux<String> stream = chatClient.prompt()
                .user("你好，请用一句话介绍你自己")
                .stream()
                .content();

        stream.doOnNext(chunk -> {
            System.out.print(chunk);
            chunkCount.incrementAndGet();
        }).blockLast();

        System.out.println("\n\n共收到 " + chunkCount.get() + " 个 chunk");
        assertTrue(chunkCount.get() > 1, "流式响应应返回多个 chunk");
    }
}
