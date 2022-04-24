package com.howtech.posstorageapi.models.store;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.*;

/**
 * @author Maurice Kelly
 * @apiNote this entity maps to the address information table of the store
 */

@Data
@Entity
@Table(name = "address_info")
public class AccountAddressInfo {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long addressId;

    @Column(name = "street")
    private String street;

    @Column(name = "city")
    private String city;

    @Column(name = "province")
    private String province;

    @Column(name = "country")
    private String country;

    @Column(name = "postcode")
    private String postCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @JsonIgnore
    private AccountInfo accountInfo;

    public AccountAddressInfo() {}

    public AccountAddressInfo(String street, String city, String province, String country, String postCode) {
        super();
        this.street = street;
        this.city = city;
        this.province = province;
        this.country = country;
        this.postCode = postCode;
    }
}
