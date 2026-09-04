package com.offerpilot.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/decision")
public class DecisionController {
 private final JdbcTemplate jdbc;
 public DecisionController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @GetMapping("/preferences") public Map<String,Object> preferences(Authentication a){return jdbc.queryForList("SELECT income,growth,stability,balance FROM decision_preferences WHERE user_id=?",UUID.fromString(a.getName())).stream().findFirst().orElse(Map.of("income",25,"growth",25,"stability",25,"balance",25));}
 @PutMapping("/preferences") public Map<String,Object> save(@Valid @RequestBody Preferences p,Authentication a){
  if(p.income()+p.growth()+p.stability()+p.balance()!=100)throw new IllegalArgumentException("权重合计必须为100");
  jdbc.update("INSERT INTO decision_preferences VALUES (?,?,?,?,?) ON CONFLICT(user_id) DO UPDATE SET income=EXCLUDED.income,growth=EXCLUDED.growth,stability=EXCLUDED.stability,balance=EXCLUDED.balance",UUID.fromString(a.getName()),p.income(),p.growth(),p.stability(),p.balance());return preferences(a);
 }
 @GetMapping("/details") public List<Map<String,Object>> details(Authentication a){return jdbc.queryForList("SELECT offer_id,hours,days,commute,benefits FROM offer_work_details WHERE user_id=?",UUID.fromString(a.getName()));}
 @PutMapping("/details/{id}") public void details(@PathVariable UUID id,@Valid @RequestBody Details d,Authentication a){
  int updated=jdbc.update("INSERT INTO offer_work_details(user_id,offer_id,hours,days,commute,benefits) SELECT ?,id,?,?,?,? FROM offers WHERE id=? ON CONFLICT(user_id,offer_id) DO UPDATE SET hours=EXCLUDED.hours,days=EXCLUDED.days,commute=EXCLUDED.commute,benefits=EXCLUDED.benefits",UUID.fromString(a.getName()),d.hours(),d.days(),d.commute(),d.benefits()==null?"":d.benefits(),id);
  if(updated==0)throw new IllegalArgumentException("Offer不存在");
 }
 public record Preferences(@Min(0) @Max(100) int income,@Min(0) @Max(100) int growth,@Min(0) @Max(100) int stability,@Min(0) @Max(100) int balance){}
 public record Details(@DecimalMin("0.5") @DecimalMax("24") Double hours,@DecimalMin("0.5") @DecimalMax("7") Double days,@DecimalMin("0") @DecimalMax("6") Double commute,@Size(max=2000) String benefits){}
}
