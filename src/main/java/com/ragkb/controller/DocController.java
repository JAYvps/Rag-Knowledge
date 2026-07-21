// ============ controller/DocController.java ============
package com.ragkb.controller;

import com.ragkb.common.Result;
import com.ragkb.entity.UserDocument;
import com.ragkb.security.UserDetailsImpl;
import com.ragkb.service.DocUploadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
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

//    @GetMapping("/preview")
//    public ResponseEntity<byte[]> preview(@RequestParam("id") Long id) throws IOException {
//        UserDocument doc = docUploadService.getById(id);
//        String filePath = doc.getFilePath();
//        File file = new File(filePath);
//        if (!file.exists()) {
//            return ResponseEntity.notFound().build();
//        }
//        String fileName = file.getName();
//        byte[] bytes = Files.readAllBytes(file.toPath());
//        // 根据后缀手动设置 Content-Type
//        String contentType = getContentType(fileName);
//        return ResponseEntity.ok()
//                .contentType(MediaType.parseMediaType(contentType))
//                .header(HttpHeaders.CACHE_CONTROL,
//                        "no-store, no-cache; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
//                .body(bytes);
//    }

    @GetMapping("/preview")
    public void preview(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        UserDocument doc = docUploadService.getById(id);
        String filePath = doc.getFilePath();
        File file = new File(filePath);
        if (!file.exists()) {
            return ;
        }
        String fileName = file.getName();
        byte[] bytes = Files.readAllBytes(file.toPath());
        // 根据后缀手动设置 Content-Type
        String contentType = "Content-Disposition: inline";
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(fileName, "UTF-8"));
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    private String getContentType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (ext) {
            case "pdf"  -> "application/pdf";
            case "png"  -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif"  -> "image/gif";
            case "txt"  -> "text/plain";
            case "html" -> "text/html";
            case "csv"  -> "text/csv";
            default     -> "application/octet-stream";
        };
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

    /**
     * 管理员设置文档为全局/取消全局
     *
     * PUT /api/admin/doc/{id}/global
     * Body: { "isGlobal": true/false }
     *
     * 仅管理员可操作，文档状态必须为"就绪"(status=2)
     */
    @PutMapping("/admin/doc/{id}/global")
    public Result<Void> setGlobal(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetailsImpl user) {
        Boolean isGlobal = body.get("isGlobal");
        if (isGlobal == null) {
            return Result.fail("参数错误");
        }
        docUploadService.setGlobal(id, isGlobal);
        return Result.ok();
    }
}
