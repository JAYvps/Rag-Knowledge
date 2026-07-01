// ============ controller/DocController.java ============
package com.ragkb.controller;

import com.ragkb.common.Result;
import com.ragkb.entity.UserDocument;
import com.ragkb.security.UserDetailsImpl;
import com.ragkb.service.DocUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 用户文档管理接口
 *
 * POST   /api/doc/upload            上传文档
 * GET    /api/doc/list              我的文档列表
 * GET    /api/doc/{id}/status       文档处理状态
 * DELETE /api/doc/{id}              删除文档
 */
@RestController
@RequestMapping("/api/doc")
@RequiredArgsConstructor
public class DocController {

    private final DocUploadService docUploadService;

    /**
     * 上传文档
     *
     * POST /api/doc/upload
     * Content-Type: multipart/form-data
     *
     * 参数:
     *   file  - 文件（PDF/TXT/MD，最大10MB）
     *   title - 标题（可选，默认用文件名）
     */
    @PostMapping("/upload")
    public Result<UserDocument> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @AuthenticationPrincipal UserDetailsImpl user) {

        UserDocument doc = docUploadService.upload(user.getUserId(), file, title);
        return Result.ok(doc);
    }

    /**
     * 获取我的文档列表
     *
     * GET /api/doc/list
     */
    @GetMapping("/list")
    public Result<List<UserDocument>> list(
            @AuthenticationPrincipal UserDetailsImpl user) {
        return Result.ok(docUploadService.listByUser(user.getUserId()));
    }

    /**
     * 获取文档处理状态
     *
     * GET /api/doc/{id}/status
     *
     * 返回:
     *   status: 0待处理 1处理中 2就绪 3失败
     *   chunkCount: 分块数
     *   wordCount: 字数
     *   errorMsg: 失败原因（仅status=3时有值）
     */
    @GetMapping("/{id}/status")
    public Result<Map<String, Object>> status(@PathVariable Long id) {
        UserDocument doc = docUploadService.getById(id);
        if (doc == null) {
            return Result.fail("文档不存在");
        }
        return Result.ok(Map.of(
                "status", doc.getStatus(),
                "chunkCount", doc.getChunkCount() != null ? doc.getChunkCount() : 0,
                "wordCount", doc.getWordCount() != null ? doc.getWordCount() : 0,
                "errorMsg", doc.getErrorMsg() != null ? doc.getErrorMsg() : ""
        ));
    }

    /**
     * 删除文档
     *
     * DELETE /api/doc/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl user) {
        docUploadService.deleteDoc(user.getUserId(), id);
        return Result.ok();
    }
}
