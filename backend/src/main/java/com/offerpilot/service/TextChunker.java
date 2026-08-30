package com.offerpilot.service;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {
    private static final int MAX_CHARS = 900;
    private static final int OVERLAP_CHARS = 140;

    public List<Chunk> split(String raw) {
        String text = raw == null ? "" : raw.replace("\r", "").replaceAll("[ \\t]+", " ").trim();
        if (text.isBlank()) return List.of();
        List<String> paragraphs = List.of(text.split("\\n{2,}"));
        List<Chunk> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String p = paragraph.trim();
            if (p.isEmpty()) continue;
            if (current.length() + p.length() + 2 > MAX_CHARS && !current.isEmpty()) {
                add(result, current.toString());
                String overlap = tail(current.toString(), OVERLAP_CHARS);
                current = new StringBuilder(overlap);
            }
            if (p.length() > MAX_CHARS) {
                for (int start = 0; start < p.length(); start += MAX_CHARS - OVERLAP_CHARS) {
                    int end = Math.min(p.length(), start + MAX_CHARS);
                    add(result, p.substring(start, end));
                    if (end == p.length()) break;
                }
                current = new StringBuilder();
            } else {
                if (!current.isEmpty()) current.append("\n\n");
                current.append(p);
            }
        }
        if (!current.isEmpty()) add(result, current.toString());
        return result;
    }

    private void add(List<Chunk> chunks, String content) {
        String clean = content.trim();
        if (!clean.isEmpty()) chunks.add(new Chunk(chunks.size(), clean, Math.max(1, clean.length() / 2)));
    }
    private String tail(String value, int size) {
        if (value.length() <= size) return value;
        int start = value.length() - size;
        int boundary = Math.max(value.lastIndexOf('。', start), value.lastIndexOf('\n', start));
        return value.substring(boundary >= 0 ? boundary + 1 : start).trim();
    }
    public record Chunk(int index, String content, int tokenEstimate) {}
}
