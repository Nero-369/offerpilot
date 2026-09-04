package com.offerpilot.api;

import com.offerpilot.service.UploadedDocumentService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resume")
public class ResumeController {
    private final JdbcTemplate jdbc;
    private final UploadedDocumentService parser;
    public ResumeController(JdbcTemplate jdbc, UploadedDocumentService parser) { this.jdbc=jdbc; this.parser=parser; }
    @GetMapping public Map<String,Object> get(Authentication auth) {
        return jdbc.queryForList("SELECT filename,content,updated_at FROM user_resumes WHERE user_id=?", UUID.fromString(auth.getName()))
            .stream().findFirst().orElse(Map.of());
    }
    @PostMapping public Map<String,Object> upload(@RequestParam("file") MultipartFile file, Authentication auth) {
        var parsed=parser.parse(file);
        jdbc.update("INSERT INTO user_resumes(user_id,filename,content) VALUES (?,?,?) ON CONFLICT(user_id) DO UPDATE SET filename=EXCLUDED.filename,content=EXCLUDED.content,updated_at=now()",
            UUID.fromString(auth.getName()),parsed.filename(),parsed.content());
        return get(auth);
    }
    @DeleteMapping public void delete(Authentication auth) {
        jdbc.update("DELETE FROM user_resumes WHERE user_id=?",UUID.fromString(auth.getName()));
    }
}
