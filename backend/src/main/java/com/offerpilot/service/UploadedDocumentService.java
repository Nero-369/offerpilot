package com.offerpilot.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Service
public class UploadedDocumentService {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md", "markdown", "csv");

    public ParsedDocument parse(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择要上传的文件");
        if (file.getSize() > MAX_BYTES) throw new IllegalArgumentException("文件不能超过 10 MB");
        String filename = file.getOriginalFilename() == null ? "uploaded" : file.getOriginalFilename();
        String extension = extension(filename);
        try {
            String text = switch (extension) {
                case "pdf" -> parsePdf(file.getBytes());
                case "docx" -> parseDocx(file.getBytes());
                default -> {
                    if (!TEXT_EXTENSIONS.contains(extension))
                        throw new IllegalArgumentException("暂不支持 ." + extension + "，请上传 PDF、DOCX、TXT、MD 或 CSV");
                    yield new String(file.getBytes(), StandardCharsets.UTF_8);
                }
            };
            text = normalize(text);
            if (text.length() < 20) throw new IllegalArgumentException("没有识别出足够文字；扫描版 PDF 或图片需要 OCR");
            if (text.length() > 100000) text = text.substring(0, 100000);
            return new ParsedDocument(filename, stripExtension(filename), text, extension);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("文件解析失败：" + e.getMessage(), e);
        }
    }

    private String parsePdf(byte[] bytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String parseDocx(byte[] bytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String normalize(String text) {
        return text.replace('\u0000', ' ').replace("\r\n", "\n")
                .replaceAll("[\\t\\x0B\\f]+", " ").replaceAll("[ ]{2,}", " ")
                .replaceAll("\\n{3,}", "\n\n").trim();
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? name : name.substring(0, dot);
    }

    public record ParsedDocument(String filename, String suggestedTitle, String content, String fileType) {}
}
