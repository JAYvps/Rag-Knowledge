// ============ controller/SyncController.java ============
package com.ragkb.controller;

import com.ragkb.common.Result;
import com.ragkb.entity.YuqueRepo;
import com.ragkb.mapper.YuqueRepoMapper;
import com.ragkb.service.YuqueSyncService;
import com.ragkb.service.YuqueSyncService.SyncResult;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 同步管理接口（管理员专用）
 *
 * 功能：
 * 1. 自动发现：导入自己语雀账户下的知识库
 * 2. 手动添加：添加互联网上的公开知识库
 * 3. 同步操作：触发单个/全部知识库的增量同步
 * 4. 状态查看：查看所有知识库的同步状态
 */
@Slf4j
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SyncController {

    private final YuqueSyncService yuqueSyncService;
    private final YuqueRepoMapper yuqueRepoMapper;

    @Value("${yuque.namespace:}")
    private String ownNamespace;

    /**
     * 自动发现自己的语雀知识库
     *
     * POST /api/sync/discover
     */
    @PostMapping("/discover")
    public Result<Map<String, Object>> discover() {
        int added = yuqueSyncService.discoverOwnRepos(ownNamespace);
        List<YuqueRepo> repos = yuqueRepoMapper.selectEnabled();
        return Result.ok(Map.of(
                "added", added,
                "total", repos.size(),
                "repos", repos
        ));
    }

    /**
     * 添加互联网公开知识库
     *
     * POST /api/sync/add-public
     */
    @PostMapping("/add-public")
    public Result<YuqueRepo> addPublicRepo(@RequestBody AddPublicRequest req) {
        YuqueRepo repo = yuqueSyncService.addPublicRepo(
                req.getNamespace(), req.getSlug(), req.getName()
        );
        return Result.ok(repo);
    }

    /**
     * 获取所有同步配置列表
     *
     * GET /api/sync/repos
     */
    @GetMapping("/repos")
    public Result<List<YuqueRepo>> listAll() {
        return Result.ok(yuqueRepoMapper.selectList(null));
    }

    /**
     * 同步单个知识库
     *
     * POST /api/sync/repo/{repoId}
     */
    @PostMapping("/repo/{repoId}")
    public Result<SyncResult> syncRepo(@PathVariable Long repoId) {
        YuqueRepo repo = yuqueRepoMapper.selectById(repoId);
        if (repo == null) {
            return Result.fail("知识库不存在");
        }
        if (repo.getSyncStatus() == 1) {
            return Result.fail("正在同步中，请稍后");
        }

        SyncResult result = yuqueSyncService.syncRepo(repoId);
        return Result.ok(result);
    }

    /**
     * 同步所有知识库
     *
     * POST /api/sync/all
     */
    @PostMapping("/all")
    public Result<Map<String, SyncResult>> syncAll() {
        Map<String, SyncResult> results = yuqueSyncService.syncAll();
        return Result.ok(results);
    }

    /**
     * 添加公开知识库的请求体
     */
    @Data
    public static class AddPublicRequest {
        @NotBlank(message = "namespace不能为空")
        private String namespace;

        @NotBlank(message = "slug不能为空")
        private String slug;

        /** 展示名称（可选） */
        private String name;
    }
}
