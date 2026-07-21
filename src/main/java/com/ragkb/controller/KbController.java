// ============ controller/KbController.java ============
package com.ragkb.controller;

import com.ragkb.common.Result;
import com.ragkb.entity.UserDocument;
import com.ragkb.entity.YuqueRepo;
import com.ragkb.entity.YuqueDocSync;
import com.ragkb.service.KbService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库浏览接口
 *
 * 提供给前端的接口：
 * 1. GET /api/kb/repos       — 知识库列表（侧边栏用）
 * 2. GET /api/kb/{id}/docs   — 某个知识库的文档列表
 * 3. GET /api/kb/global-docs — 全局文档列表
 */
@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
public class KbController {

    private final KbService kbService;

    /**
     * 获取所有知识库列表
     *
     * 返回启用状态的知识库，按排序字段升序
     * 前端侧边栏渲染用
     */
    @GetMapping("/repos")
    public Result<List<YuqueRepo>> listRepos() {
        return Result.ok(kbService.listRepos());
    }

    /**
     * 获取某个知识库下的文档列表
     *
     * @param repoId 知识库ID（yuque_repo表的id）
     * 返回该知识库下已同步的文档列表
     */
    @GetMapping("/{repoId}/docs")
    public Result<List<YuqueDocSync>> listDocs(@PathVariable Long repoId) {
        return Result.ok(kbService.listDocs(repoId));
    }

    /**
     * 获取全局文档列表
     *
     * 返回所有管理员设为全局的文档（is_global=1, status=2）
     * 所有用户都可以查看
     */
    @GetMapping("/global-docs")
    public Result<List<UserDocument>> listGlobalDocs() {
        return Result.ok(kbService.listGlobalDocs());
    }
}
