// ============ config/RestTemplateConfig.java ============
package com.ragkb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate — 调用语雀API用
     *
     * 设置较长的超时时间，因为语雀大文档的body可能很大
     * 连接超时5秒，读取超时30秒
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        //最大连接等待时长
        factory.setConnectTimeout(5000);
        //最大读取时长
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }
}
