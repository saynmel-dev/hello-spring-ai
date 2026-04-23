package com.example.springaidemo.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

/**
 * 敏感词拦截 Advisor。
 * <p>
 * 同时实现 {@link CallAdvisor} 和 {@link StreamAdvisor}，
 * 在请求发送到模型之前检查用户输入是否包含敏感词。
 * <ul>
 *   <li>如果检测到敏感词，直接拦截请求并返回提示信息，不会调用模型</li>
 *   <li>如果未检测到敏感词，正常放行请求</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 使用默认敏感词列表
 * new SensitiveWordAdvisor();
 *
 * // 自定义敏感词列表
 * new SensitiveWordAdvisor(List.of("暴力", "赌博"), 0);
 * }</pre>
 */
public class SensitiveWordAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordAdvisor.class);

    /** 检测到敏感词时返回给用户的默认提示 */
    private static final String DEFAULT_REJECT_MESSAGE = "您的输入包含敏感词，请修改后重试。";

    /** 内置的默认敏感词列表（演示用途） */
    private static final List<String> DEFAULT_SENSITIVE_WORDS = Arrays.asList(
            "暴力", "赌博", "毒品"
    );

    private final List<String> sensitiveWords;
    private final String rejectMessage;
    private final int order;

    /**
     * 使用默认敏感词列表和默认拒绝提示。
     */
    public SensitiveWordAdvisor() {
        this(DEFAULT_SENSITIVE_WORDS, DEFAULT_REJECT_MESSAGE, 0);
    }

    /**
     * 自定义敏感词列表。
     *
     * @param sensitiveWords 敏感词列表
     * @param order          Advisor 执行顺序
     */
    public SensitiveWordAdvisor(List<String> sensitiveWords, int order) {
        this(sensitiveWords, DEFAULT_REJECT_MESSAGE, order);
    }

    /**
     * 完全自定义配置。
     *
     * @param sensitiveWords 敏感词列表
     * @param rejectMessage  检测到敏感词时的拒绝提示
     * @param order          Advisor 执行顺序
     */
    public SensitiveWordAdvisor(List<String> sensitiveWords, String rejectMessage, int order) {
        this.sensitiveWords = sensitiveWords;
        this.rejectMessage = rejectMessage;
        this.order = order;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    // ==================== 同步调用拦截 ====================

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String detected = detectSensitiveWord(request);
        if (detected != null) {
            log.warn("[敏感词拦截] 检测到敏感词「{}」，请求已拦截", detected);
            throw new SensitiveWordException(rejectMessage, detected);
        }

        return chain.nextCall(request);
    }

    // ==================== 流式调用拦截 ====================

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String detected = detectSensitiveWord(request);
        if (detected != null) {
            log.warn("[敏感词拦截-Stream] 检测到敏感词「{}」，请求已拦截", detected);
            return Flux.error(new SensitiveWordException(rejectMessage, detected));
        }

        return chain.nextStream(request);
    }

    // ==================== 敏感词检测 ====================

    /**
     * 检查请求中的用户消息是否包含敏感词。
     *
     * @param request 聊天请求
     * @return 命中的敏感词，未命中返回 null
     */
    private String detectSensitiveWord(ChatClientRequest request) {
        if (request.prompt() == null || request.prompt().getInstructions() == null) {
            return null;
        }

        for (Message message : request.prompt().getInstructions()) {
            if (message instanceof UserMessage && message.getText() != null) {
                String text = message.getText();
                for (String word : sensitiveWords) {
                    if (text.contains(word)) {
                        return word;
                    }
                }
            }
        }

        return null;
    }

    // ==================== 敏感词异常 ====================

    /**
     * 敏感词拦截异常。
     * <p>
     * 当用户输入包含敏感词时抛出，调用方可通过捕获此异常进行统一处理。
     */
    public static class SensitiveWordException extends RuntimeException {

        private final String sensitiveWord;

        public SensitiveWordException(String message, String sensitiveWord) {
            super(message);
            this.sensitiveWord = sensitiveWord;
        }

        /**
         * 获取触发拦截的敏感词。
         */
        public String getSensitiveWord() {
            return sensitiveWord;
        }
    }
}
