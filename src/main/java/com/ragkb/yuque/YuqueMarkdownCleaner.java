// ============ yuque/YuqueMarkdownCleaner.java ============
package com.ragkb.yuque;

/**
 * 语雀Markdown预处理
 *
 * 语雀有自己的扩展Markdown语法，直接做Embedding会有很多噪音
 * 这个类负责清理，提取对RAG有用的纯文本
 */
public class YuqueMarkdownCleaner {

    /**
     * 清理语雀Markdown
     *
     * 处理规则：
     * 1. 保留有意义的文字
     * 2. 移除格式标记（保留#标题，对分块有帮助）
     * 3. 移除语雀特有的嵌入/画板等
     * 4. 图片保留alt文字，链接保留文字
     */
    public static String clean(String markdown) {
        if (markdown == null) {
            return "";
        }

        String text = markdown;

        // ---- 语雀特有标记 ----

        // 提示框: {% note warning %}内容{% endnote %}
        text = text.replaceAll("\\{%\\s*note[^%]*%\\}", "");
        text = text.replaceAll("\\{%\\s*endnote\\s*%\\}", "");

        // 嵌入块: {% embed url %}
        text = text.replaceAll("\\{%\\s*embed[^%]*%\\}", "");

        // 画板/脑图/序列图
        text = text.replaceAll(
                "\\{%\\s*(mindmap|mermaid|sequence|flowchart)[^%]*%\\}[\\s\\S]*?\\{%\\s*end\\w+\\s*%\\}",
                ""
        );

        // ---- Markdown标记清理 ----

        // 图片: ![alt](url) -> 保留alt文字
        text = text.replaceAll("!\\[([^\\]]*)\\]\\([^)]*\\)", "$1");

        // 链接: [text](url) -> 保留文字
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1");

        // HTML标签
        text = text.replaceAll("<[^>]+>", " ");

        // 行内代码: `code` -> 保留内容
        text = text.replaceAll("`([^`]+)`", "$1");

        // 代码块标记
        text = text.replaceAll("```[a-zA-Z]*\\n?", "");
        text = text.replaceAll("```", "");

        // 粗体/斜体: **bold** *italic* -> 保留内容
        text = text.replaceAll("\\*{1,2}([^*]+)\\*{1,2}", "$1");

        // 删除线: ~~text~~
        text = text.replaceAll("~~([^~]+)~~", "$1");

        // ---- 清理空白 ----

        // 合并连续空格（保留换行）
        text = text.replaceAll("[ \\t]+", " ");

        // 最多两个连续换行
        text = text.replaceAll("\\n{3,}", "\n\n");

        return text.trim();
    }
}
