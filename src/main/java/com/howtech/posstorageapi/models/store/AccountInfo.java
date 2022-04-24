package com.howtech.posstorageapi.models.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.howtech.posstorageapi.models.store.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "account")
public class AccountInfo {
    @Id
    @GeneratedValue
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "routing_number")
    private Long routingNumber;

    @Column(name = "account_type")
    private AccountType accountType;

    @Column(name = "created_at")
    private Date accountStartDate = new Date(System.currentTimeMillis());

    @OneToOne(mappedBy = "accountInfo", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private AccountAddressInfo accountAddressInfo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @JsonIgnore
    private Store store;


}
