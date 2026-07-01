// ============ service/EmbeddingService.java ============
package com.ragkb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本向量化服务
 *
 * 阿里云text-embedding-v3免费版限制：
 * - 每次最多10条文本
 * - 每条文本最多512 tokens
 * 所以批量向量化时需要分批调用
 */
@Slf4j
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /** 阿里云免费版每次最多10条 */
    private static final int MAX_BATCH_SIZE = 10;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 将单条文本转为向量
     */
    public float[] embed(String text) {
        float[] vector = embeddingModel.embed(text);
        log.debug("向量化完成: textLen={}, vectorDim={}", text.length(), vector.length);
        return vector;
    }

    /**
     * 批量向量化（自动分批，每批最多10条）
     *
     * 阿里云限制每次最多10条，所以：
     * 92条文本 → 分10批（9批×10条 + 1批×2条）
     * 每批之间间隔200ms避免触发限流
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        log.info("开始批量向量化: {} 条文本, 每批{}条", texts.size(), MAX_BATCH_SIZE);

        List<float[]> allVectors = new ArrayList<>();

        // 分批处理
        for (int i = 0; i < texts.size(); i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, texts.size());
            List<String> batch = texts.subList(i, end);

            log.info("向量化批次: {}/{} (本批{}条)",
                    i / MAX_BATCH_SIZE + 1,
                    (texts.size() + MAX_BATCH_SIZE - 1) / MAX_BATCH_SIZE,
                    batch.size());

            EmbeddingRequest request = new EmbeddingRequest(batch, null);
            EmbeddingResponse response = embeddingModel.call(request);

            for (var result : response.getResults()) {
                float[] doubles = result.getOutput();
                float[] floats = new float[doubles.length];
                for (int j = 0; j < doubles.length; j++) {
                    floats[j] = (float) doubles[j];
                }
                allVectors.add(floats);
            }

            // 批次间休息，避免限流
            if (end < texts.size()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.info("批量向量化完成: {} 条, 维度={}",
                allVectors.size(),
                allVectors.isEmpty() ? 0 : allVectors.get(0).length);

        return allVectors;
    }
}
