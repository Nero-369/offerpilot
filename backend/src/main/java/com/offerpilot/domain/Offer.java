package com.offerpilot.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offers")
public class Offer {
    @Id private UUID id;
    @Column(nullable = false) private String company;
    @Column(nullable = false) private String role;
    @Column(nullable = false) private String city;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal monthlySalary;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal salaryMonths;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal annualBonus;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal annualHousingCost;
    @Column(length = 8000) private String jobDescription;
    @Column(nullable = false) private Instant createdAt;

    protected Offer() {}
    public Offer(String company, String role, String city, BigDecimal monthlySalary, BigDecimal salaryMonths, BigDecimal annualBonus, BigDecimal annualHousingCost, String jobDescription) {
        this.id = UUID.randomUUID(); this.company = company; this.role = role; this.city = city;
        this.monthlySalary = monthlySalary; this.salaryMonths = salaryMonths; this.annualBonus = annualBonus;
        this.annualHousingCost = annualHousingCost; this.jobDescription = jobDescription; this.createdAt = Instant.now();
    }
    public UUID getId(){return id;} public String getCompany(){return company;} public String getRole(){return role;} public String getCity(){return city;}
    public BigDecimal getMonthlySalary(){return monthlySalary;} public BigDecimal getSalaryMonths(){return salaryMonths;} public BigDecimal getAnnualBonus(){return annualBonus;}
    public BigDecimal getAnnualHousingCost(){return annualHousingCost;} public String getJobDescription(){return jobDescription;} public Instant getCreatedAt(){return createdAt;}
}
