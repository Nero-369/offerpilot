package com.offerpilot.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/regions")
public class RegionController {
    private final JdbcTemplate jdbc;
    public RegionController(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @GetMapping
    public List<RegionOption> search(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="30") int limit){
        int safeLimit=Math.max(1,Math.min(limit,100));String keyword="%"+q.trim()+"%";
        return jdbc.query("""
          SELECT code,province,city,baseline_year,source_url FROM china_regions
          WHERE (?='' OR city ILIKE ? OR province ILIKE ?)
          ORDER BY CASE WHEN city=? THEN 0 WHEN city LIKE ? THEN 1 ELSE 2 END,province,city
          LIMIT ?
          """,(rs,n)->new RegionOption(rs.getString("code"),rs.getString("province"),rs.getString("city"),rs.getInt("baseline_year"),rs.getString("source_url")),q.trim(),keyword,keyword,q.trim(),q.trim()+"%",safeLimit);
    }
    public record RegionOption(String code,String province,String city,int baselineYear,String sourceUrl){}
}
