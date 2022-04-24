package com.howtech.posstorageapi.DTOs;

import lombok.Data;

/**
 * @author Maurice Kelly
 */
@Data
public class CompanyDto {
    private String name;
    private String email;
    private String ein;
    private String industry;
    private String url_description;
    private String phoneNumber;
    private String vat_no;
    private String billing_line1;
    private String billing_line2;
    private String billing_city;
    private String billing_province;
    private String billing_country;
    private String billing_postcode;

    private String accountNo;
    private String routingNo;

}
