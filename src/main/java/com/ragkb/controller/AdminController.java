package com.ragkb.controller;

import com.ragkb.common.Result;
import com.ragkb.entity.YuqueDocSync;
import com.ragkb.entity.YuqueRepo;
import com.ragkb.security.UserDetailsImpl;
import com.ragkb.service.KbService;
import com.ragkb.service.YuqueSyncService;
import com.ragkb.service.YuqueSyncService.SyncResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final YuqueSyncService yuqueSyncService;
    private final KbService kbService;

    /**
     * 获取所有知识库（含禁用的）
     */
    @GetMapping("/repos")
    public Result<List<YuqueRepo>> listAllRepos(
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return Result.fail("未登录");
        }

        if(user.getUserId() == 1){
            List<YuqueRepo> repos = kbService.listAllRepos();
            return Result.ok(repos);
        }
        return Result.fail("无权限");
    }

    /**
     * 添加语雀知识库
     *
     * 参数：
     * - namespace:    语雀用户名（必填）
     * - slug:         知识库slug（必填）
     * - articleSlug:  文章slug（选填，填了只同步该文章）
     * - name:         显示名称（选填）
     */
    @PostMapping("/repo/add")
    public Result<Map<String, Object>> addRepo(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return Result.fail("未登录");
        }

        String namespace = body.get("namespace");
        String slug = body.get("slug");
        String articleSlug = body.get("articleSlug");
        String name = body.get("name");

        if (namespace == null || namespace.isBlank()
                || slug == null || slug.isBlank()) {
            return Result.fail("请填写 namespace 和 slug");
        }

        try {
            // 1. 查找或创建知识库
            YuqueRepo repo = yuqueSyncService.findOrCreateRepo(namespace, slug, name);

            // 2. 同步
            SyncResult syncResult;
            if (articleSlug != null && !articleSlug.isBlank()) {
                // 只同步指定文章
                syncResult = yuqueSyncService.syncSingleDoc(repo.getId(), articleSlug);
            } else {
                // 同步整个知识库
                syncResult = yuqueSyncService.syncRepo(repo.getId());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("repo", repo);
            result.put("syncResult", syncResult);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("添加知识库失败: {}/{}", namespace, slug, e);
            return Result.fail("添加失败: " + e.getMessage());
        }
    }



    /**
     * 触发单个知识库同步
     */
    @PostMapping("/repo/sync/{id}")
    public Result<SyncResult> syncRepo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return Result.fail("未登录");
        }
        try {
            SyncResult result = yuqueSyncService.syncRepo(id);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("同步失败: id={}", id, e);
            return Result.fail("同步失败: " + e.getMessage());
        }
    }

    /**
     * 触发全部同步
     */
    @PostMapping("/repo/sync-all")
    public Result<Map<String, SyncResult>> syncAll(
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return Result.fail("未登录");
        }
        try {
            Map<String, SyncResult> results = yuqueSyncService.syncAll();
            return Result.ok(results);
        } catch (Exception e) {
            log.error("全部同步失败", e);
            return Result.fail("同步失败: " + e.getMessage());
        }
    }

    /**
     * 获取某知识库下的文档列表
     */
    @GetMapping("/repo/{id}/docs")
    public Result<List<YuqueDocSync>> listRepoDocs(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return Result.fail("未登录");
        }
        return Result.ok(kbService.listDocs(id));
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/repo/{id}")
    public Result<Void> deleteRepo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return Result.fail("未登录");
        }
        try {
            yuqueSyncService.deleteRepo(id);
            return Result.ok();
        } catch (Exception e) {
            log.error("删除失败: id={}", id, e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }
}
