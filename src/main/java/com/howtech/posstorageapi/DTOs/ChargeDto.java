package com.howtech.posstorageapi.DTOs;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Maurice Kelly
 */
@Data
public class ChargeDto {
    private BigDecimal amount;
    private String currency;
    private String description;
    private Long orderId;
}
