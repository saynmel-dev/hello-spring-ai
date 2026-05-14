package com.example.springaidemo.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * Re-Read (RE2) 重读提示 Advisor。
 * <p>
 * 基于 {@link BaseAdvisor} 实现，利用其模板方法模式，
 * 只需关注 {@code before()} 和 {@code after()} 的逻辑，
 * 无需手动处理同步/流式调用的差异。
 * <p>
 * 实现 RE2 (Re-Reading) 提示策略：在用户问题之后追加一条
 * "Read the question again:" + 原始问题 的指令，引导模型重新审视问题，
 * 从而提升推理准确性。
 * <p>
 * 论文参考：<a href="https://arxiv.org/abs/2309.06275">RE2: Re-Reading Improves Reasoning in Large Language Models</a>
 *
 * <p>原理：
 * <ul>
 *   <li>原始输入：Q（用户问题）</li>
 *   <li>RE2 增强后：Q + "Read the question again: " + Q</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * ChatClient.builder(chatModel)
 *     .build()
 *     .prompt()
 *     .advisors(new ReReadAdvisor())
 *     .user("12个球中有一个重量不同，用天平称3次找出来")
 *     .call()
 *     .content();
 * }</pre>
 */
public class ReReadAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ReReadAdvisor.class);

    /** 默认的重读引导前缀 */
    private static final String DEFAULT_REREAD_PREFIX = "Read the question again:";

    private final String rereadPrefix;
    private final int order;

    /**
     * 使用默认配置创建 ReReadAdvisor。
     */
    public ReReadAdvisor() {
        this(DEFAULT_REREAD_PREFIX, 0);
    }

    /**
     * 指定执行顺序。
     *
     * @param order Advisor 执行顺序
     */
    public ReReadAdvisor(int order) {
        this(DEFAULT_REREAD_PREFIX, order);
    }

    /**
     * 完全自定义配置。
     *
     * @param rereadPrefix 重读引导前缀，例如 "请重新阅读问题："
     * @param order        Advisor 执行顺序
     */
    public ReReadAdvisor(String rereadPrefix, int order) {
        this.rereadPrefix = rereadPrefix;
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    // ==================== BaseAdvisor 模板方法 ====================

    /**
     * 请求前处理：应用 RE2 重读策略，在消息末尾追加重读指令。
     */
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        if (request.prompt() == null || request.prompt().getInstructions() == null) {
            return request;
        }

        // 提取用户消息文本
        String userQuestion = extractUserQuestion(request.prompt().getInstructions());
        if (userQuestion == null || userQuestion.isBlank()) {
            log.debug("[ReRead] 未找到用户消息，跳过重读增强");
            return request;
        }

        log.info("[ReRead] 应用重读策略，原始问题: {}",
                userQuestion.length() > 100 ? userQuestion.substring(0, 100) + "..." : userQuestion);

        // 构建增强后的消息列表
        List<Message> enhancedMessages = new ArrayList<>(request.prompt().getInstructions());

        // 追加重读指令作为新的 UserMessage
        String rereadInstruction = rereadPrefix + "\n" + userQuestion;
        enhancedMessages.add(new UserMessage(rereadInstruction));

        // 构建新的 Prompt，保留原始参数
        Prompt enhancedPrompt = new Prompt(enhancedMessages, request.prompt().getOptions());

        // 使用 mutate 构建新的 request，保留原始 context
        return request.mutate()
                .prompt(enhancedPrompt)
                .build();
    }

    /**
     * 响应后处理：直接透传，不做额外处理。
     */
    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    // ==================== 工具方法 ====================

    /**
     * 从消息列表中提取最后一条用户消息的文本。
     *
     * @param messages 消息列表
     * @return 用户问题文本，未找到返回 null
     */
    private String extractUserQuestion(List<Message> messages) {
        // 从后往前找最后一条 UserMessage
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message instanceof UserMessage && message.getText() != null) {
                return message.getText();
            }
        }
        return null;
    }
}
