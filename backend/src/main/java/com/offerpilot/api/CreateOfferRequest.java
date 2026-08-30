package com.offerpilot.api;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record CreateOfferRequest(
    @NotBlank String company, @NotBlank String role, @NotBlank String city,
    @NotNull @Positive BigDecimal monthlySalary,
    @NotNull @DecimalMin("12.0") @DecimalMax("24.0") BigDecimal salaryMonths,
    @NotNull @PositiveOrZero BigDecimal annualBonus,
    @NotNull @PositiveOrZero BigDecimal annualHousingCost,
    @Size(max=8000) String jobDescription) {}
