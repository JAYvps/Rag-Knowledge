package com.ragkb.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI相关配置
 *
 * Spring AI会自动配置ChatModel（读取spring.ai.openai.*配置）
 * 这里把ChatModel包装成ChatClient，方便使用
 */
@Configuration
public class AiConfig {

    /**
     * ChatClient：Spring AI的高级对话接口
     *
     * 用法：
     *   chatClient.prompt()
     *       .system("你是知识库助手...")
     *       .user("什么是Transformer？")
     *       .stream()
     *       .content()  → Flux<String> 流式返回
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
