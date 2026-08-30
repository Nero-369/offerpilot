package com.offerpilot.api;
import java.math.BigDecimal;
import java.util.List;
public record DecisionReport(
    BigDecimal grossAnnualIncome,
    BigDecimal estimatedTaxAndSocialInsurance,
    BigDecimal disposableAnnualIncome,
    int jobMatchScore,
    int growthScore,
    int stabilityScore,
    int confidence,
    String recommendation,
    List<String> strengths,
    List<String> risks,
    List<Evidence> evidence) {
    public record Evidence(String title, String source, String effectiveDate, String confidenceLevel) {}
}
