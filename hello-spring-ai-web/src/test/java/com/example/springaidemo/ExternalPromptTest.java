package com.example.springaidemo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 加载外部提示词模板测试。
 *
 * 演示如何从 classpath 资源文件（.st 模板）加载系统提示词，
 * 实现提示词与代码解耦，便于维护和复用。
 *
 * 模板文件位于 src/main/resources/prompts/system/ 目录下，
 * 使用 Spring AI 的 PromptTemplate 支持变量占位符替换。
 */
@SpringBootTest
class ExternalPromptTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    // 通过 @Value 注入 classpath 资源文件
    @Value("classpath:prompts/system/translator.st")
    private Resource translatorPrompt;

    @Value("classpath:prompts/system/code-reviewer.st")
    private Resource codeReviewerPrompt;

    @Value("classpath:prompts/system/simple-assistant.st")
    private Resource simpleAssistantPrompt;

    /**
     * 加载无参数的外部提示词模板
     */
    @Test
    void testLoadSimpleExternalPrompt() {
        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
                .system(simpleAssistantPrompt)
                .user("什么是Spring Boot？")
                .call()
                .content();

        System.out.println("简单外部提示词回复: " + response);

        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 加载带参数的翻译助手模板，通过 PromptTemplate 填充变量
     */
    @Test
    void testExternalTranslatorPromptWithParams() {
        // 使用 PromptTemplate 渲染模板
        PromptTemplate promptTemplate = new PromptTemplate(translatorPrompt);
        String systemPromptText = promptTemplate.render(Map.of(
                "source_lang", "中文",
                "target_lang", "英文"
        ));

        System.out.println("渲染后的系统提示词:\n" + systemPromptText);

        ChatClient chatClient = chatClientBuilder
                .defaultSystem(systemPromptText)
                .build();

        String response = chatClient.prompt()
                .user("微服务是一种将应用程序构建为一组小型服务的架构风格。")
                .call()
                .content();

        System.out.println("翻译结果: " + response);

        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 加载代码审查专家模板，动态指定编程语言和输出风格
     */
    @Test
    void testExternalCodeReviewerPrompt() {
        PromptTemplate promptTemplate = new PromptTemplate(codeReviewerPrompt);
        String systemPromptText = promptTemplate.render(Map.of(
                "language", "Java",
                "output_style", "简洁的列表"
        ));

        System.out.println("渲染后的系统提示词:\n" + systemPromptText);

        ChatClient chatClient = chatClientBuilder
                .defaultSystem(systemPromptText)
                .build();

        String codeSnippet = """
                public String getUser(int id) {
                    String sql = "SELECT * FROM users WHERE id = " + id;
                    return db.query(sql);
                }
                """;

        String response = chatClient.prompt()
                .user("请审查以下代码:\n" + codeSnippet)
                .call()
                .content();

        System.out.println("代码审查结果: " + response);

        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 使用 prompt().system(Resource) 直接传入资源 + 参数填充
     * 这是 ChatClient 原生支持的方式，更简洁
     */
    @Test
    void testSystemPromptResourceWithParams() {
        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
                .system(s -> s.text(translatorPrompt)
                        .param("source_lang", "英文")
                        .param("target_lang", "中文"))
                .user("Microservices is an architectural style that structures an application as a collection of small services.")
                .call()
                .content();

        System.out.println("Resource参数化翻译结果: " + response);

        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    /**
     * 流式输出 + 外部提示词模板
     */
    @Test
    void testStreamWithExternalPrompt() {
        PromptTemplate promptTemplate = new PromptTemplate(codeReviewerPrompt);
        String systemPromptText = promptTemplate.render(Map.of(
                "language", "Python",
                "output_style", "详细的分析报告"
        ));

        ChatClient chatClient = chatClientBuilder
                .defaultSystem(systemPromptText)
                .build();

        StringBuilder fullResponse = new StringBuilder();

        chatClient.prompt()
                .user("请审查: def divide(a, b): return a / b")
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
