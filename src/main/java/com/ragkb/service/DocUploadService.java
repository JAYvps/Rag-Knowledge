package com.ragkb.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ragkb.common.BusinessException;
import com.ragkb.common.TextChunkUtil;
import com.ragkb.common.TextChunkUtil.TextChunk;
import com.ragkb.entity.DocumentChunk;
import com.ragkb.entity.UserDocument;
import com.ragkb.mapper.DocumentChunkMapper;
import com.ragkb.mapper.UserDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// spring poi 允许上传docx/xlsx
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户文档上传服务
 *
 * 流程：
 * 1. 校验文件（类型、大小）
 * 2. 保存文件到本地
 * 3. 创建数据库记录（status=0 待处理）
 * 4. 异步处理：解析 → 分块 → 向量化 → 存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocUploadService {

    private final UserDocumentMapper userDocMapper;
    private final DocumentChunkMapper chunkMapper;
    private final EmbeddingService embeddingService;
    private final VectorService vectorService;

    @Value("${upload.path:./data/upload}")
    private String uploadPath;

    @Value("${upload.max-size:10485760}")
    private long maxSize;

    /** 允许的文件类型 不允许旧版本doc*/
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "pdf", "txt", "md", "docx", "xlsx"
    );


    // ==================== 上传 ====================

    /**
     * 上传文档
     *
     * @param userId 当前用户ID
     * @param file   上传的文件
     * @param title  展示标题（可选，默认用文件名）
     * @return 文档记录
     */
    public UserDocument upload(Long userId, MultipartFile file, String title) {
        // 1. 基本校验
        String ext = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_TYPES.contains(ext)) {
            throw new BusinessException("仅支持 PDF、TXT、MD、DOCX、XLSX 格式");
        }

        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小不能超过10MB");
        }

        // 2. 保存文件到本地
        String storedFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String filePath = Path.of(uploadPath, storedFileName).toString();
        try {
            Files.createDirectories(Path.of(uploadPath));
            file.transferTo(Path.of(filePath));
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败");
        }

        // 3. 创建数据库记录
        UserDocument doc = new UserDocument();
        doc.setUserId(userId);
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(ext);
        doc.setFileSize(file.getSize());
        doc.setFilePath(filePath);
        doc.setTitle(title != null && !title.isBlank()
                ? title : file.getOriginalFilename());
        doc.setStatus(0); // 待处理
        userDocMapper.insert(doc);

        // 4. 异步处理
        processAsync(doc.getId());

        return doc;
    }

    // ==================== 查询 ====================

    /**
     * 获取用户的文档列表
     */
    public List<UserDocument> listByUser(Long userId) {
        return userDocMapper.selectList(
                new LambdaQueryWrapper<UserDocument>()
                        .eq(UserDocument::getUserId, userId)
                        .orderByDesc(UserDocument::getCreatedAt)
        );
    }

    /**
     * 获取单个文档
     */
    public UserDocument getById(Long docId) {
        return userDocMapper.selectById(docId);
    }

    // ==================== 删除 ====================

    /**
     * 删除用户文档（同时删除向量和文件）
     */
    public void deleteDoc(Long userId, Long docId) {
        UserDocument doc = userDocMapper.selectById(docId);
        if (doc == null) {
            throw new BusinessException("文档不存在");
        }
        if (!doc.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此文档");
        }

        // 1. 删除向量
        List<DocumentChunk> chunks = chunkMapper.selectBySource("user", docId);
        List<String> vectorIds = chunks.stream()
                .map(DocumentChunk::getVectorId)
                .filter(Objects::nonNull)
                .toList();
        if (!vectorIds.isEmpty()) {
            vectorService.deleteByIds(vectorIds);
        }

        // 2. 删除MySQL分块记录
        chunkMapper.deleteBySource("user", docId);

        // 3. 删除文件
        try {
            Files.deleteIfExists(Path.of(doc.getFilePath()));
        } catch (IOException e) {
            log.warn("文件删除失败: {}", doc.getFilePath());
        }

        // 4. 删除文档记录
        userDocMapper.deleteById(docId);
    }

    // ==================== 异步处理 ====================

    /**
     * 异步处理文档：解析 → 分块 → 向量化
     *
     * 用 @Async 在独立线程中执行，不阻塞用户请求
     * 前端通过轮询 /doc/{id}/status 查看处理进度
     */
    @Async("docProcessExecutor")
    public void processAsync(Long docId) {
        UserDocument doc = userDocMapper.selectById(docId);
        if (doc == null) {
            return;
        }

        log.info("[文档处理] 开始: id={}, file={}", docId, doc.getFileName());

        // 更新状态：处理中
        doc.setStatus(1);
        userDocMapper.updateById(doc);

        try {
            // 1. 解析文件，提取纯文本
            String text = parseFile(doc.getFilePath(), doc.getFileType());
            log.info("[文档处理] 解析完成: textLen={}", text.length());

            // 2. 分块
            List<TextChunk> chunks = TextChunkUtil.split(doc.getTitle(), text);
            log.info("[文档处理] 分块完成: chunks={}", chunks.size());

            // 3. 批量向量化
            List<String> texts = chunks.stream()
                    .map(TextChunk::content)
                    .toList();
            List<float[]> vectors = embeddingService.embedBatch(texts);
            log.info("[文档处理] 向量化完成: vectors={}", vectors.size());

            // 4. 逐条存入向量库 + 保存MySQL记录
            for (int i = 0; i < chunks.size(); i++) {
                TextChunk chunk = chunks.get(i);

                // 构建元数据
                Map<String, String> metadata = new HashMap<>();
                metadata.put("sourceType", "user");
                metadata.put("sourceId", String.valueOf(docId));
                metadata.put("userId", String.valueOf(doc.getUserId()));
                metadata.put("docTitle", doc.getTitle());
                metadata.put("fileName", doc.getFileName());
                metadata.put("chunkIndex", String.valueOf(i));

                // 存入向量库
                String vectorId = vectorService.store(
                        chunk.content(), vectors.get(i), metadata
                );

                // 保存MySQL分块记录
                DocumentChunk record = new DocumentChunk();
                record.setSourceType("user");
                record.setSourceId(docId);
                record.setChunkIndex(i);
                record.setContent(chunk.content());
                record.setTokenCount(chunk.tokenCount());
                record.setVectorId(vectorId);
                chunkMapper.insert(record);
            }

            // 5. 更新状态：就绪
            doc.setStatus(2);
            doc.setChunkCount(chunks.size());
            doc.setWordCount(text.length());
            userDocMapper.updateById(doc);

            log.info("[文档处理] 完成: id={}, chunks={}, wordCount={}",
                    docId, chunks.size(), text.length());

        } catch (Exception e) {
            log.error("[文档处理] 失败: id={}", docId, e);
            doc.setStatus(3);
            doc.setErrorMsg(e.getMessage());
            userDocMapper.updateById(doc);
        }
    }

    // ==================== 文件解析 ====================

    /**
     * 根据文件类型分发解析
     */
    private String parseFile(String filePath, String fileType) {
        return switch (fileType) {
            case "pdf" -> parsePdf(filePath);
            case "txt" -> parseTxt(filePath);
            case "md"  -> parseMd(filePath);
            case "docx" -> parseDocx(filePath);
            case "xlsx" -> parseXlsx(filePath);
            default -> throw new BusinessException("不支持的文件格式: " + fileType);
        };
    }


    /**
     * 解析PDF（使用Apache PDFBox）
     */
    private String parsePdf(String filePath) {
        try (PDDocument pdDoc = Loader.loadPDF(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdDoc);
            return cleanText(text);
        } catch (IOException e) {
            throw new BusinessException("PDF解析失败: " + e.getMessage());
        }
    }

    /**
     * 读取TXT
     */
    private String parseTxt(String filePath) {
        try {
            String text = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
            return cleanText(text);
        } catch (IOException e) {
            throw new BusinessException("TXT读取失败: " + e.getMessage());
        }
    }

    /**
     * 读取MD（保留Markdown标记，对RAG有帮助）
     */
    private String parseMd(String filePath) {
        try {
            String text = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
            // MD文件保留#标题标记（对分块有帮助），只清理图片和链接URL
            text = text.replaceAll("!\\[([^\\]]*)\\]\\([^)]*\\)", "$1");
            text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1");
            return cleanText(text);
        } catch (IOException e) {
            throw new BusinessException("MD读取失败: " + e.getMessage());
        }
    }
    /**
     * 解析Word文档 (.docx)
     */
    private String parseDocx(String filePath) {
        try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(Path.of(filePath)))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                sb.append(para.getText()).append("\n");
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        sb.append(cell.getText()).append(" | ");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
            String text = cleanText(sb.toString());
            if (text.length() > 500_000) {
                text = text.substring(0, 500_000);
                log.warn("Word内容过长，已截断: {}", filePath);
            }
            return text;
        } catch (IOException e) {
            throw new BusinessException("Word文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析Excel表格 (.xlsx)
     */
    private String parseXlsx(String filePath) {
        try (Workbook wb = WorkbookFactory.create(new File(filePath))) {
            StringBuilder sb = new StringBuilder();
            for (Sheet sheet : wb) {
                sb.append("## ").append(sheet.getSheetName()).append("\n\n");
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        sb.append(getCellText(cell)).append(" | ");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
            String text = cleanText(sb.toString());
            if (text.length() > 500_000) {
                text = text.substring(0, 500_000);
                log.warn("Excel内容过长，已截断: {}", filePath);
            }
            return text;
        } catch (IOException e) {
            throw new BusinessException("Excel解析失败: " + e.getMessage());
        }
    }

    /**
     * 提取Excel单元格文本
     */
    private String getCellText(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }


    /**
     * 通用文本清理
     */
    private String cleanText(String text) {
        if (text == null) return "";
        text = text.replaceAll("<[^>]+>", " ");
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text.trim();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
