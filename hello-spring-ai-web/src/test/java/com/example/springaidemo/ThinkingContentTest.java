package com.example.springaidemo;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 打印模型的思考（reasoning/thinking）内容。
 *
 * DeepSeek 系列模型（尤其是 R1）会在响应中返回 reasoning_content 字段，
 * Spring AI 会将其映射到 AssistantMessage 的 metadata 中。
 *
 * 注意：并非所有模型都支持思考内容输出，DeepSeek-V3 可能不返回该字段，
 * 如需测试建议切换到 DeepSeek-R1 模型。
 */
@SpringBootTest
class ThinkingContentTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void printThinkingContent() {
        ChatClient chatClient = chatClientBuilder.build();

        ChatResponse chatResponse = chatClient.prompt()
                .user("9.11 和 9.8 哪个大？请仔细思考后回答。")
                .call()
                .chatResponse();

        assertNotNull(chatResponse);

        Generation generation = chatResponse.getResult();
        assertNotNull(generation);

        AssistantMessage assistantMessage = generation.getOutput();
        assertNotNull(assistantMessage);

        // 打印最终回复
        String content = assistantMessage.getText();
        System.out.println("====== 最终回复 ======");
        System.out.println(content);

        // 打印思考内容（reasoning_content）
        Map<String, Object> metadata = assistantMessage.getMetadata();
        System.out.println("\n====== Message Metadata ======");
        if (metadata != null && !metadata.isEmpty()) {
            metadata.forEach((key, value) -> {
                System.out.println(key + " -> " + value);
            });
        } else {
            System.out.println("（无 metadata）");
        }

        // 尝试从 metadata 中获取 reasoningContent
        // Spring AI OpenAI 适配器会将 reasoning_content 映射为 "reasoningContent" key
        Object reasoningContent = metadata != null ? metadata.get("reasoningContent") : null;
        System.out.println("\n====== 思考过程（Reasoning Content）======");
        if (reasoningContent != null) {
            System.out.println(reasoningContent);
        } else {
            System.out.println("（模型未返回思考内容，可能需要使用支持 reasoning 的模型如 DeepSeek-R1）");
        }

        // 打印 Generation 级别的 metadata
        var genMetadata = generation.getMetadata();
        System.out.println("\n====== Generation Metadata ======");
        if (genMetadata != null) {
            System.out.println("FinishReason: " + genMetadata.getFinishReason());
        } else {
            System.out.println("（无 generation metadata）");
        }

        // 打印 ChatResponse 级别的 metadata
        System.out.println("\n====== ChatResponse Metadata ======");
        var responseMetadata = chatResponse.getMetadata();
        if (responseMetadata != null) {
            System.out.println("Model: " + responseMetadata.getModel());
            System.out.println("Usage - PromptTokens: " + responseMetadata.getUsage().getPromptTokens());
            System.out.println("Usage - CompletionTokens: " + responseMetadata.getUsage().getCompletionTokens());
            System.out.println("Usage - TotalTokens: " + responseMetadata.getUsage().getTotalTokens());
        }

        assertNotNull(content);
        assertFalse(content.isBlank());
    }
}
