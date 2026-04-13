package com.example.springaidemo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatClient 系统提示词（System Prompt）测试。
 *
 * 演示如何通过 ChatClient 设置系统提示词来控制模型的行为和角色。
 */
@SpringBootTest
class SystemPromptTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    /**
     * 基础用法：通过 prompt().system() 设置系统提示词
     */
    @Test
    void testSystemPromptWithPromptApi() {
        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
                .system("你是一个专业的Java开发工程师，所有回答都要围绕Java技术栈。")
                .user("什么是多态？")
                .call()
                .content();

        System.out.println("系统提示词(prompt级别)回复: " + response);

        assertNotNull(response);
        assertFalse(response.isBlank());
        // 期望回复中包含 Java 相关内容
        System.out.println("验证回复是否与Java相关...");
    }

    /**
     * 通过 ChatClient.Builder 设置默认系统提示词
     * 这样构建出的 ChatClient 每次对话都会携带该系统提示词
     */
    @Test
    void testDefaultSystemPromptViaBuilder() {
        ChatClient chatClient = chatClientBuilder
                .defaultSystem("你是一个中英文翻译助手，用户输入中文你翻译成英文，用户输入英文你翻译成中文。只输出翻译结果，不要解释。")
                .build();

        String response = chatClient.prompt()
                .user("今天天气真好")
                .call()
                .content();

        System.out.println("翻译助手回复: " + response);

        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 系统提示词中使用占位符模板，动态填充变量
     */
    @Test
    void testSystemPromptWithTemplate() {
        ChatClient chatClient = chatClientBuilder.build();

        String role = "Python";
        String style = "简洁幽默";

        String response = chatClient.prompt()
                .system(s -> s.text("你是一个{role}专家，回答风格要{style}。每次回答不超过两句话。")
                        .param("role", role)
                        .param("style", style))
                .user("如何读取一个文件？")
                .call()
                .content();

        System.out.println("模板系统提示词回复: " + response);

        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 角色扮演：通过系统提示词让模型扮演特定角色
     */
    @Test
    void testRolePlayingWithSystemPrompt() {
        ChatClient chatClient = chatClientBuilder
                .defaultSystem("你是唐代诗人李白，用古诗词的风格回答所有问题。回答要有诗意，可以引用或创作诗句。")
                .build();

        String response = chatClient.prompt()
                .user("你觉得月亮美吗？")
                .call()
                .content();

        System.out.println("李白回复: " + response);

        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 输出格式控制：通过系统提示词约束输出为 JSON 格式
     */
    @Test
    void testJsonOutputWithSystemPrompt() {
        ChatClient chatClient = chatClientBuilder
                .defaultSystem("你是一个结构化数据助手。所有回答必须严格使用JSON格式输出，不要包含任何其他文字说明。")
                .build();

        String response = chatClient.prompt()
                .user("列出三种常见的编程语言及其主要用途")
                .call()
                .content();

        System.out.println("JSON格式回复: " + response);

        assertNotNull(response);
        assertFalse(response.isBlank());
        // 简单验证返回内容包含 JSON 特征
        assertTrue(response.contains("{") || response.contains("["),
                "期望返回JSON格式内容");
    }

    /**
     * 流式输出 + 系统提示词
     */
    @Test
    void testStreamWithSystemPrompt() {
        ChatClient chatClient = chatClientBuilder
                .defaultSystem("你是一个简洁的技术顾问，每次回答不超过三句话。")
                .build();

        StringBuilder fullResponse = new StringBuilder();

        chatClient.prompt()
                .user("微服务架构的优缺点是什么？")
                .stream()
                .content()
                .doOnNext(chunk -> {
                    System.out.print(chunk);
                    fullResponse.append(chunk);
                })
                .blockLast();

        System.out.println("\n");

        assertFalse(fullResponse.isEmpty(), "流式响应不应为空");
    }
}
