package com.howtech.posstorageapi.DTOs;

import com.howtech.posstorageapi.models.store.enums.AccountType;
import com.howtech.posstorageapi.models.store.enums.ChargeFrequency;
import com.howtech.posstorageapi.models.store.enums.MembershipType;
import lombok.Data;

/**
 * @author Maurice Kelly
 */
@Data
public class StoreDto {

    private String accountManager;
    private String workPhone;
    private String cellPhone;
    //owner name add to array
    private String ownerFirstName;
    private String ownerLastName;
    //json ignore on pass when returning jwt request

    private String storeName;
    private boolean queueFull;
    private boolean openForDelivery;
    private MembershipType membershipType;
    private String membershipCode;
    private ChargeFrequency whenToCharge;

    //address information
    private String owner_address_street;
    private String owner_address_city;
    private String owner_address_province;
    private String owner_address_country;
    private String owner_address_postcode;

    private String store_address_street;
    private String store_address_city;
    private String store_address_province;
    private String store_address_country;
    private String store_address_postcode;

    private String business_ein;
    private String business_storeName;

    private String account_address_street;
    private String account_address_city;
    private String account_address_province;
    private String account_address_country;
    private String account_address_postcode;

    private AccountType account_type;


}
