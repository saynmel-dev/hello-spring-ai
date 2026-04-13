package com.example.springaidemo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多平台、多模型动态调用 ChatClient 测试。
 *
 * 核心思路：利用 Spring AI 的 OpenAI 兼容适配器，通过动态构建
 * OpenAiApi -> OpenAiChatModel -> ChatClient 来对接不同平台和模型。
 *
 * 支持所有兼容 OpenAI API 协议的平台，如：
 * - DeepSeek 官方 / 百炼托管
 * - 通义千问（DashScope）
 * - Moonshot（Kimi）
 * - OpenAI 官方
 * - 其他兼容平台
 */
@SpringBootTest
class MultiPlatformChatTest {

    // ==================== 平台配置 ====================
    // 请根据实际情况修改 API Key 和 Base URL

    /**
     * 模型配置记录
     */
    record ModelConfig(String platform, String baseUrl, String apiKey, String model) {
        @Override
        public String toString() {
            return platform + " / " + model;
        }
    }

    /**
     * 定义多平台多模型配置。
     * 实际使用时请替换为真实的 API Key。
     */
    static Stream<Arguments> modelConfigs() {
        return Stream.of(
                // ---- 平台1: 当前项目默认配置（百炼 DeepSeek）----
                Arguments.of(new ModelConfig(
                        "百炼-DeepSeek",
                        env("OPENAI_BASE_URL", "https://uat-aibrain-large-model-engine.hellobike.cn"),
                        env("OPENAI_API_KEY", ""),
                        env("OPENAI_MODEL", "DeepSeek-V3.2-Bailian")
                )),

                // ---- 平台2: DeepSeek 官方 ----
                Arguments.of(new ModelConfig(
                        "DeepSeek官方",
                        "https://api.deepseek.com",
                        env("DEEPSEEK_API_KEY", ""),
                        "deepseek-chat"
                )),

                // ---- 平台3: 通义千问 DashScope ----
                Arguments.of(new ModelConfig(
                        "通义千问",
                        "https://dashscope.aliyuncs.com/compatible-mode/v1",
                        env("DASHSCOPE_API_KEY", ""),
                        "qwen-plus"
                )),

                // ---- 平台4: Moonshot (Kimi) ----
                Arguments.of(new ModelConfig(
                        "Moonshot-Kimi",
                        "https://api.moonshot.cn/v1",
                        env("MOONSHOT_API_KEY", ""),
                        "moonshot-v1-8k"
                ))
        );
    }

    /**
     * 从环境变量读取配置，支持默认值
     */
    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    // ==================== 动态构建 ChatClient ====================

    /**
     * 根据配置动态创建 ChatClient（核心方法）
     */
    private ChatClient buildChatClient(ModelConfig config) {
        // 1. 构建 OpenAI 兼容 API 客户端
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .build();

        // 2. 构建 ChatModel，指定默认模型
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(config.model())
                        .temperature(0.7)
                        .maxTokens(1024)
                        .build())
                .build();

        // 3. 构建 ChatClient
        return ChatClient.builder(chatModel).build();
    }

    // ==================== 测试用例 ====================

    /**
     * 参数化测试：遍历所有平台模型，验证基本对话能力
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("modelConfigs")
    void testMultiPlatformChat(ModelConfig config) {
        skipIfNoApiKey(config);

        ChatClient chatClient = buildChatClient(config);

        System.out.println("\n====== 测试平台: " + config + " ======");

        String response = chatClient.prompt()
                .user("你好，请用一句话介绍你自己，并说明你是哪个模型。")
                .call()
                .content();

        System.out.println("回复: " + response);

        assertNotNull(response, config + " 返回了 null");
        assertFalse(response.isBlank(), config + " 返回了空内容");
    }

    /**
     * 参数化测试：验证各平台流式输出
     */
    @ParameterizedTest(name = "[{index}] 流式-{0}")
    @MethodSource("modelConfigs")
    void testMultiPlatformStreamChat(ModelConfig config) {
        skipIfNoApiKey(config);

        ChatClient chatClient = buildChatClient(config);

        System.out.println("\n====== 流式测试: " + config + " ======");

        StringBuilder fullResponse = new StringBuilder();

        chatClient.prompt()
                .user("用三句话解释什么是大语言模型。")
                .stream()
                .content()
                .doOnNext(chunk -> {
                    System.out.print(chunk);
                    fullResponse.append(chunk);
                })
                .blockLast();

        System.out.println("\n");

        assertFalse(fullResponse.isEmpty(), config + " 流式响应为空");
    }

    /**
     * 参数化测试：验证各平台返回的 ChatResponse 元数据
     */
    @ParameterizedTest(name = "[{index}] 元数据-{0}")
    @MethodSource("modelConfigs")
    void testMultiPlatformChatResponseMetadata(ModelConfig config) {
        skipIfNoApiKey(config);

        ChatClient chatClient = buildChatClient(config);

        System.out.println("\n====== 元数据测试: " + config + " ======");

        ChatResponse chatResponse = chatClient.prompt()
                .user("1+1等于几？")
                .call()
                .chatResponse();

        assertNotNull(chatResponse);
        assertNotNull(chatResponse.getResult());

        String content = chatResponse.getResult().getOutput().getText();
        System.out.println("回复: " + content);

        // 打印 token 用量
        var usage = chatResponse.getMetadata().getUsage();
        System.out.println("PromptTokens: " + usage.getPromptTokens());
        System.out.println("CompletionTokens: " + usage.getCompletionTokens());
        System.out.println("TotalTokens: " + usage.getTotalTokens());
        System.out.println("Model: " + chatResponse.getMetadata().getModel());

        assertNotNull(content);
        assertTrue(usage.getTotalTokens() > 0, "Token 用量应大于 0");
    }

    /**
     * 单测试：同一个 prompt 发给所有可用平台，横向对比输出
     */
    @Test
    void compareAllPlatformResponses() {
        String prompt = "用一句话回答：为什么天空是蓝色的？";
        Map<String, String> results = new LinkedHashMap<>();

        modelConfigs().forEach(args -> {
            ModelConfig config = (ModelConfig) args.get()[0];
            if (config.apiKey().isBlank()) {
                results.put(config.toString(), "[跳过 - 未配置 API Key]");
                return;
            }

            try {
                ChatClient chatClient = buildChatClient(config);
                String response = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();
                results.put(config.toString(), response);
            } catch (Exception e) {
                results.put(config.toString(), "[错误] " + e.getMessage());
            }
        });

        System.out.println("\n====== 多平台横向对比 ======");
        System.out.println("Prompt: " + prompt);
        System.out.println("─".repeat(60));
        results.forEach((platform, response) -> {
            System.out.println("【" + platform + "】");
            System.out.println("  " + response);
            System.out.println();
        });

        // 至少有一个平台成功返回
        long successCount = results.values().stream()
                .filter(r -> !r.startsWith("["))
                .count();
        assertTrue(successCount >= 1, "至少应有一个平台成功返回结果");
    }

    /**
     * 单测试：动态切换模型 —— 同一平台不同模型对比
     */
    @Test
    void testDynamicModelSwitching() {
        String baseUrl = env("OPENAI_BASE_URL", "https://uat-aibrain-large-model-engine.hellobike.cn");
        String apiKey = env("OPENAI_API_KEY", "");

        if (apiKey.isBlank()) {
            System.out.println("跳过：未配置 OPENAI_API_KEY");
            return;
        }

        // 同一平台下的不同模型
        String[] models = {
                env("OPENAI_MODEL", "DeepSeek-V3.2-Bailian"),
                // 如有其他可用模型，在此添加
                // "DeepSeek-R1-Bailian",
                // "qwen-plus",
        };

        String prompt = "用一句话解释量子计算。";

        System.out.println("====== 同平台多模型动态切换 ======");
        System.out.println("Base URL: " + baseUrl);
        System.out.println("Prompt: " + prompt);
        System.out.println("─".repeat(60));

        for (String model : models) {
            ModelConfig config = new ModelConfig("动态切换", baseUrl, apiKey, model);
            ChatClient chatClient = buildChatClient(config);

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            System.out.println("【" + model + "】" + response);
            System.out.println();

            assertNotNull(response);
            assertFalse(response.isBlank());
        }
    }

    // ==================== 辅助方法 ====================

    private void skipIfNoApiKey(ModelConfig config) {
        if (config.apiKey().isBlank()) {
            System.out.println("跳过 " + config + "：未配置 API Key");
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                    "跳过 " + config + "：未配置 API Key");
        }
    }
}
