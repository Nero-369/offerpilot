package com.offerpilot.service;
import com.offerpilot.domain.Offer;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
class IncomeCalculatorTest {
    private final IncomeCalculator calculator=new IncomeCalculator();
    @Test void calculatesDeterministicDisposableIncome(){
        Offer offer=new Offer("云栖网络","AI全栈工程师","杭州",new BigDecimal("22000"),new BigDecimal("14"),new BigDecimal("20000"),new BigDecimal("36000"),"Spring AI");
        IncomeCalculator.Result result=calculator.calculate(offer);
        assertThat(result.grossAnnualIncome()).isEqualByComparingTo("328000.00");
        assertThat(result.disposableAnnualIncome()).isPositive().isLessThan(result.grossAnnualIncome());
    }
}
