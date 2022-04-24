package com.howtech.posstorageapi.DTOs;

import lombok.Data;

/**
 * @author Maurice Kelly
 */
@Data
public class PayerDto {
    private String name;
    private String email;
    private String phoneNumber;
    private String billing_line1;
    private String billing_line2;
    private String billing_city;
    private String billing_province;
    private String billing_country;
    private String billing_postcode;

    private String accountNo;
    private String routingNo;

    private Long cardNumber;
    private int expiryMonth;
    private int expiryYear;
    private int cvc;

    private String pay_processor_id;

}
