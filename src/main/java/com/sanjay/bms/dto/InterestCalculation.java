package com.sanjay.bms.dto;

import lombok.*;
import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InterestCalculation {
    private BigDecimal principal;
    private BigDecimal rate;
    private BigDecimal time;
    private Integer frequency;
    private BigDecimal simpleInterest;
    private BigDecimal compoundInterest;
    private BigDecimal maturityAmount;
}