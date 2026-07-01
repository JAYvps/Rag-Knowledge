// ============ config/VectorStoreConfig.java ============
package com.ragkb.config;

import com.ragkb.common.LocalVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储配置
 *
 * 使用纯Java本地实现，不依赖外部向量数据库
 * 数据持久化到本地JSON文件
 */
@Configuration
public class VectorStoreConfig {

    @Value("${vector.store.path:./data/vectors}")
    private String vectorStorePath;

    /**
     * 创建本地向量存储实例
     *
     * 启动时自动从文件加载已有向量数据
     * 每次增删操作后自动保存到文件
     */
    @Bean
    public LocalVectorStore localVectorStore() {
        return new LocalVectorStore(vectorStorePath);
    }
}
