// ============ controller/TestController.java ============
package com.ragkb.controller;

import com.ragkb.common.Result;
import com.ragkb.service.EmbeddingService;
import com.ragkb.service.VectorService;
import com.ragkb.service.VectorService.VectorSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 测试接口（验证向量检索效果）
 * 上线前删除此Controller
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final EmbeddingService embeddingService;
    private final VectorService vectorService;

    /**
     * 测试向量检索
     *
     * GET /api/test/search?question=什么是Transformer
     */
    @GetMapping("/search")
    public Result<List<VectorSearchResult>> testSearch(
            @RequestParam String question,
            @RequestParam(defaultValue = "5") int topK) {

        // 1. 把问题向量化
        float[] questionVector = embeddingService.embed(question);

        // 2. 向量检索
        List<VectorSearchResult> results = vectorService.search(question, questionVector, topK);

        return Result.ok(results);
    }

    /**
     * 查看向量库状态
     *
     * GET /api/test/status
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.ok(Map.of(
                "totalVectors", vectorService.size()
        ));
    }
}
