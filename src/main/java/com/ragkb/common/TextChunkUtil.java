package com.ragkb.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块工具类
 *
 * 策略：
 * 1. 先按Markdown标题（# ## ###）切分
 * 2. 超长章节按段落继续切分
 * 3. 每块前加上文档标题作为上下文
 * 4. 相邻块之间有重叠
 */
public class TextChunkUtil {

    /** 每块最大字符数 */
    private static final int MAX_CHUNK_SIZE = 500;

    /** 相邻块重叠字符数 */
    private static final int OVERLAP = 50;

    /**
     * 分块结果
     */
    public record TextChunk(String content, int tokenCount) {}

    /**
     * 文本分块
     *
     * @param title 文档标题（第一块会带上标题作为上下文）
     * @param text  清理后的纯文本
     * @return 分块列表
     */
    public static List<TextChunk> split(String title, String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<TextChunk> chunks = new ArrayList<>();

        // 第1步：按Markdown标题切分
        List<String> sections = splitByHeading(text);

        for (String section : sections) {
            if (section.length() <= MAX_CHUNK_SIZE) {
                String content = prependTitle(title, section);
                chunks.add(new TextChunk(content, content.length()));
            } else {
                // 超长章节按段落切
                List<String> subs = splitByParagraph(section, MAX_CHUNK_SIZE, OVERLAP);
                for (String sub : subs) {
                    String content = prependTitle(title, sub);
                    chunks.add(new TextChunk(content, content.length()));
                }
            }
        }

        // 至少保留一块
        if (chunks.isEmpty()) {
            String content = prependTitle(title, text);
            chunks.add(new TextChunk(content, content.length()));
        }

        return chunks;
    }

    /**
     * 按Markdown标题行切分
     */
    private static List<String> splitByHeading(String text) {
        String[] lines = text.split("\n");
        List<String> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (line.matches("^#{1,6}\\s+.+")) {
                if (current.length() > 0) {
                    sections.add(current.toString().trim());
                    current = new StringBuilder();
                }
            }
            current.append(line).append("\n");
        }

        if (current.length() > 0) {
            sections.add(current.toString().trim());
        }

        return sections;
    }

    /**
     * 按段落切分（处理超长章节）
     */
    private static List<String> splitByParagraph(String text, int maxSize, int overlap) {
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            if (current.length() + para.length() + 2 > maxSize && current.length() > 0) {
                result.add(current.toString().trim());
                String tail = current.length() > overlap
                        ? current.substring(current.length() - overlap) : "";
                current = new StringBuilder(tail);
            }
            current.append(para).append("\n\n");
        }

        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    /**
     * 给分块内容加上文档标题上下文
     */
    private static String prependTitle(String title, String content) {
        if (title != null && !title.isBlank() && !content.startsWith("# ")) {
            return "## " + title + "\n\n" + content;
        }
        return content;
    }
}
