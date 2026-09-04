package com.offerpilot.service;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {
    private static final int CHILD_MAX_CHARS = 650;
    private static final int CHILD_OVERLAP_CHARS = 100;
    private static final int PARENT_MAX_CHARS = 2600;

    public List<Chunk> split(String raw) {
        return splitHierarchical(raw).stream().flatMap(parent -> parent.children().stream()).toList();
    }

    public List<ParentChunk> splitHierarchical(String raw) {
        String text = raw == null ? "" : raw.replace("\r", "").replaceAll("[ \\t]+", " ").trim();
        if (text.isBlank()) return List.of();
        List<ParentChunk> hierarchy = new ArrayList<>();
        int nextChildIndex = 0;
        List<String> blocks = parentBlocks(text);
        for (int parentIndex = 0; parentIndex < blocks.size(); parentIndex++) {
            String parent = blocks.get(parentIndex);
            List<Chunk> children = splitChildren(parent, nextChildIndex);
            nextChildIndex += children.size();
            hierarchy.add(new ParentChunk(parentIndex, sectionTitle(parent, parentIndex), parent,
                    Math.max(1, parent.length() / 2), children));
        }
        return hierarchy;
    }

    private List<Chunk> splitChildren(String text, int firstIndex) {
        List<Chunk> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : text.split("\\n{2,}")) {
            String clean = paragraph.trim();
            if (clean.isEmpty()) continue;
            if (current.length() + clean.length() + 2 > CHILD_MAX_CHARS && !current.isEmpty()) {
                add(result, current.toString(), firstIndex);
                current = new StringBuilder(tail(current.toString(), CHILD_OVERLAP_CHARS));
            }
            if (clean.length() > CHILD_MAX_CHARS) {
                for (int start = 0; start < clean.length(); start += CHILD_MAX_CHARS - CHILD_OVERLAP_CHARS) {
                    int end = Math.min(clean.length(), start + CHILD_MAX_CHARS);
                    add(result, clean.substring(start, end), firstIndex);
                    if (end == clean.length()) break;
                }
                current = new StringBuilder();
            } else {
                if (!current.isEmpty()) current.append("\n\n");
                current.append(clean);
            }
        }
        if (!current.isEmpty()) add(result, current.toString(), firstIndex);
        return result;
    }

    private List<String> parentBlocks(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : text.split("\\n{2,}")) {
            String clean = paragraph.trim();
            if (clean.isEmpty()) continue;
            boolean heading = clean.length() < 160 && clean.matches(
                    "(?s)^(#{1,6}\\s+.+|第[一二三四五六七八九十百0-9]+[章节条].*|[一二三四五六七八九十]+[、.．].*)$");
            if ((heading && !current.isEmpty()) || current.length() + clean.length() + 2 > PARENT_MAX_CHARS) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) current.append("\n\n");
            current.append(clean);
        }
        if (!current.isEmpty()) result.add(current.toString().trim());
        return result;
    }

    private String sectionTitle(String content, int index) {
        String first = content.lines().findFirst().orElse("").replaceFirst("^#{1,6}\\s*", "").trim();
        return first.length() <= 100 ? first : "第 " + (index + 1) + " 节";
    }

    private void add(List<Chunk> chunks, String content, int firstIndex) {
        String clean = content.trim();
        if (!clean.isEmpty()) chunks.add(new Chunk(firstIndex + chunks.size(), clean, Math.max(1, clean.length() / 2)));
    }

    private String tail(String value, int size) {
        if (value.length() <= size) return value;
        int start = value.length() - size;
        int boundary = Math.max(value.lastIndexOf('。', start), value.lastIndexOf('\n', start));
        return value.substring(boundary >= 0 ? boundary + 1 : start).trim();
    }

    public record Chunk(int index, String content, int tokenEstimate) {}
    public record ParentChunk(int index, String sectionTitle, String content, int tokenEstimate,
                              List<Chunk> children) {}
}
