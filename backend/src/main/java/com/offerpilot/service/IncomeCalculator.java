package com.offerpilot.service;

import com.offerpilot.domain.Offer;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class IncomeCalculator {
    public Result calculate(Offer offer) {
        BigDecimal gross = offer.getMonthlySalary().multiply(offer.getSalaryMonths()).add(offer.getAnnualBonus());
        BigDecimal socialInsurance = gross.multiply(new BigDecimal("0.105"));
        BigDecimal taxable = gross.subtract(socialInsurance).subtract(new BigDecimal("60000")).max(BigDecimal.ZERO);
        BigDecimal tax = progressiveAnnualTax(taxable);
        BigDecimal disposable = gross.subtract(socialInsurance).subtract(tax).subtract(offer.getAnnualHousingCost());
        return new Result(scale(gross), scale(socialInsurance.add(tax)), scale(disposable));
    }
    private BigDecimal progressiveAnnualTax(BigDecimal taxable) {
        if (taxable.compareTo(new BigDecimal("36000")) <= 0) return taxable.multiply(new BigDecimal("0.03"));
        if (taxable.compareTo(new BigDecimal("144000")) <= 0) return taxable.multiply(new BigDecimal("0.10")).subtract(new BigDecimal("2520"));
        if (taxable.compareTo(new BigDecimal("300000")) <= 0) return taxable.multiply(new BigDecimal("0.20")).subtract(new BigDecimal("16920"));
        if (taxable.compareTo(new BigDecimal("420000")) <= 0) return taxable.multiply(new BigDecimal("0.25")).subtract(new BigDecimal("31920"));
        return taxable.multiply(new BigDecimal("0.30")).subtract(new BigDecimal("52920"));
    }
    private BigDecimal scale(BigDecimal value){return value.setScale(2, RoundingMode.HALF_UP);}
    public record Result(BigDecimal grossAnnualIncome, BigDecimal taxAndSocialInsurance, BigDecimal disposableAnnualIncome) {}
}
