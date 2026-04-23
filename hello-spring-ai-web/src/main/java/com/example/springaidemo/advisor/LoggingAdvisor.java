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
import org.springframework.ai.chat.model.ChatResponse;

import reactor.core.publisher.Flux;

/**
 * 对话日志记录 Advisor。
 * <p>
 * 同时实现 {@link CallAdvisor} 和 {@link StreamAdvisor}，
 * 在请求发送前和响应返回后记录完整的对话日志，包括：
 * <ul>
 *   <li>用户消息 / 系统消息</li>
 *   <li>模型响应内容</li>
 *   <li>Token 用量（如果可用）</li>
 *   <li>调用耗时</li>
 * </ul>
 */
public class LoggingAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(LoggingAdvisor.class);

    private final int order;

    public LoggingAdvisor() {
        this(0);
    }

    public LoggingAdvisor(int order) {
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
        logRequest(request);

        long startTime = System.currentTimeMillis();
        ChatClientResponse response = chain.nextCall(request);
        long elapsed = System.currentTimeMillis() - startTime;

        logResponse(response, elapsed);
        return response;
    }

    // ==================== 流式调用拦截 ====================

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        logRequest(request);

        long startTime = System.currentTimeMillis();

        return chain.nextStream(request)
                .doOnNext(response -> {
                    ChatResponse chatResponse = response.chatResponse();
                    if (chatResponse != null
                            && chatResponse.getResult() != null
                            && chatResponse.getResult().getOutput() != null
                            && chatResponse.getResult().getOutput().getText() != null) {
                        log.debug("[Stream Chunk] {}", chatResponse.getResult().getOutput().getText());
                    }
                })
                .doOnComplete(() -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.info("[Stream完成] 耗时: {}ms", elapsed);
                })
                .doOnError(error -> log.error("[Stream异常] {}", error.getMessage(), error));
    }

    // ==================== 日志工具方法 ====================

    private void logRequest(ChatClientRequest request) {
        log.info("========== AI 请求开始 ==========");

        // 记录 Prompt 中的所有消息
        if (request.prompt() != null && request.prompt().getInstructions() != null) {
            for (Message message : request.prompt().getInstructions()) {
                log.info("[{}] {}", message.getMessageType(), message.getText());
            }
        }

        log.info("=================================");
    }

    private void logResponse(ChatClientResponse response, long elapsed) {
        log.info("========== AI 响应结果 ==========");
        log.info("[耗时] {}ms", elapsed);

        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null) {
            log.warn("[响应] 为空");
            log.info("=================================");
            return;
        }

        // 记录响应内容
        var result = chatResponse.getResult();
        if (result != null && result.getOutput() != null) {
            String text = result.getOutput().getText();
            if (text != null) {
                if (text.length() > 500) {
                    log.info("[响应内容] {}... (共{}字符)", text.substring(0, 500), text.length());
                } else {
                    log.info("[响应内容] {}", text);
                }
            }
        }

        // 记录 Token 用量
        var metadata = chatResponse.getMetadata();
        if (metadata != null && metadata.getUsage() != null) {
            var usage = metadata.getUsage();
            log.info("[Token用量] prompt={}, completion={}, total={}",
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens());
        }

        log.info("=================================");
    }
}
