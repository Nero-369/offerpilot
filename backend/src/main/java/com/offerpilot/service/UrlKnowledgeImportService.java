package com.offerpilot.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UrlKnowledgeImportService {
    private static final int MAX_URLS = 20;
    private static final int MAX_DOWNLOAD_BYTES = 15 * 1024 * 1024;
    private static final int MAX_CONTENT_CHARS = 100_000;
    private static final Pattern DATE = Pattern.compile("(20\\d{2})[年./-](\\d{1,2})[月./-](\\d{1,2})日?");
    private static final Pattern DOCUMENT_NUMBER = Pattern.compile("([\\p{IsHan}]{1,12}[〔﹝\\[]20\\d{2}[〕﹞\\]]\\d+号)");
    private static final List<String> CITIES = List.of("北京市", "上海市", "天津市", "重庆市", "杭州市", "深圳市", "广州市", "南京市", "苏州市", "成都市", "武汉市", "西安市", "长沙市", "宁波市", "合肥市");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final KnowledgeService knowledge;

    public UrlKnowledgeImportService(KnowledgeService knowledge) { this.knowledge = knowledge; }

    public List<Preview> preview(List<String> urls) {
        if (urls == null || urls.isEmpty()) throw new IllegalArgumentException("至少填写一个网址");
        if (urls.size() > MAX_URLS) throw new IllegalArgumentException("每次最多处理20个网址");
        List<Preview> result = new ArrayList<>();
        for (String url : urls.stream().map(String::trim).filter(value -> !value.isBlank()).distinct().toList()) {
            try {
                if (knowledge.sourceExists(url)) result.add(new Preview(url, null, null, null, null, null, null, 3, null, "该网址已经入库"));
                else result.add(fetch(url));
            }
            catch (Exception exception) { result.add(new Preview(url, null, null, null, null, null, null, 3, null, error(exception))); }
        }
        return result;
    }

    private Preview fetch(String url) throws Exception {
        URI uri = validate(url);
        Download download = download(uri, 0);
        String contentType = download.contentType().toLowerCase(Locale.ROOT);
        Extracted extracted = contentType.contains("pdf") || uri.getPath().toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? extractPdf(download.bytes()) : extractHtml(download.bytes(), uri.toString());
        String content = normalize(extracted.content());
        if (content.length() < 80) throw new IllegalArgumentException("未提取到足够正文，可能是扫描PDF或需要登录的页面");
        if (content.length() > MAX_CONTENT_CHARS) content = content.substring(0, MAX_CONTENT_CHARS);
        String title = extracted.title() == null || extracted.title().isBlank() ? uri.getHost() : extracted.title();
        String combined = title + "\n" + content.substring(0, Math.min(content.length(), 5000));
        return new Preview(url, title, detectCity(combined), detectPolicyType(combined), detectDate(combined), null,
                detectDocumentNumber(combined), 3, content, null);
    }

    private Download download(URI uri, int redirects) throws Exception {
        if (redirects > 3) throw new IllegalArgumentException("网址重定向次数过多");
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12))
                .header("User-Agent", "OfferPilot-Knowledge-Importer/1.0")
                .header("Accept", "text/html,application/pdf;q=0.9,*/*;q=0.5").GET().build();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 300 && response.statusCode() < 400) {
            String location = response.headers().firstValue("location").orElseThrow(() -> new IllegalArgumentException("重定向缺少地址"));
            URI next = validate(uri.resolve(location).toString());
            response.body().close();
            return download(next, redirects + 1);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            response.body().close();
            throw new IllegalArgumentException("下载失败，HTTP " + response.statusCode());
        }
        long declared = response.headers().firstValueAsLong("content-length").orElse(-1);
        if (declared > MAX_DOWNLOAD_BYTES) { response.body().close(); throw new IllegalArgumentException("文件超过15MB限制"); }
        byte[] bytes;
        try (InputStream input = response.body()) { bytes = input.readNBytes(MAX_DOWNLOAD_BYTES + 1); }
        if (bytes.length > MAX_DOWNLOAD_BYTES) throw new IllegalArgumentException("文件超过15MB限制");
        return new Download(bytes, response.headers().firstValue("content-type").orElse(""));
    }

    private URI validate(String value) throws Exception {
        URI uri = URI.create(value);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
            throw new IllegalArgumentException("只支持公网 HTTP/HTTPS 地址");
        }
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new IllegalArgumentException("不允许访问本机或内网地址");
            }
        }
        return uri;
    }

    private Extracted extractHtml(byte[] bytes, String baseUri) throws IOException {
        Document document = Jsoup.parse(new ByteArrayInputStream(bytes), null, baseUri);
        document.select("script,style,noscript,nav,footer,header,form,iframe,.nav,.footer,.sidebar,.recommend,.related").remove();
        String title = document.selectFirst("h1") == null ? document.title() : document.selectFirst("h1").text();
        List<Element> candidates = document.select("article,main,.article-content,.TRS_Editor,#zoom,.content,.pages_content");
        Element root = candidates.stream().max(Comparator.comparingInt(element -> element.text().length())).orElse(document.body());
        List<String> lines = root.select("h1,h2,h3,h4,p,li,table tr").eachText().stream()
                .map(String::trim).filter(line -> line.length() > 1).distinct().toList();
        String content = lines.isEmpty() ? root.text() : String.join("\n\n", lines);
        return new Extracted(title, content);
    }

    private Extracted extractPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String content = stripper.getText(document);
            String title = document.getDocumentInformation().getTitle();
            if (title == null || title.isBlank()) title = content.lines().filter(line -> !line.isBlank()).findFirst().orElse("PDF政策文档");
            return new Extracted(title, content);
        }
    }

    private String normalize(String value) {
        return value.replace('\u00a0', ' ').replace("\r", "").replaceAll("[ \\t]+", " ")
                .replaceAll("\\n[ \\t]*\\n[ \\t]*\\n+", "\n\n").trim();
    }

    private String detectCity(String value) { return CITIES.stream().filter(value::contains).findFirst().orElse(null); }
    private String detectPolicyType(String value) {
        if (value.contains("公积金")) return "住房公积金";
        if (value.contains("社会保险") || value.contains("社保") || value.contains("养老保险")) return "社会保险";
        if (value.contains("个人所得税") || value.contains("专项附加扣除")) return "个人所得税";
        if (value.contains("最低工资")) return "最低工资";
        if (value.contains("劳动合同") || value.contains("年休假") || value.contains("工资支付")) return "劳动权益";
        if (value.contains("人才补贴") || value.contains("生活补贴") || value.contains("就业补贴")) return "人才补贴";
        return "政策文件";
    }
    private LocalDate detectDate(String value) {
        Matcher matcher = DATE.matcher(value);
        while (matcher.find()) try { return LocalDate.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3))); }
        catch (RuntimeException ignored) { }
        return null;
    }
    private String detectDocumentNumber(String value) { Matcher matcher = DOCUMENT_NUMBER.matcher(value); return matcher.find() ? matcher.group(1) : null; }
    private String error(Exception exception) { return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(); }

    private record Download(byte[] bytes, String contentType) {}
    private record Extracted(String title, String content) {}
    public record Preview(String sourceUrl, String title, String city, String policyType, LocalDate effectiveDate,
                          LocalDate expiryDate, String versionLabel, int authorityLevel, String content, String error) {}
}
