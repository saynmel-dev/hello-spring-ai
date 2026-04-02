package com.example.springaidemo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
