package com.howtech.posstorageapi.models.store;


import com.howtech.posstorageapi.models.store.enums.ChargeFrequency;
import com.howtech.posstorageapi.models.store.enums.MembershipType;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue
    @Column(name = "store_id")
    private Long storeId;
    @Column(name = "store_name", nullable = false)
    private String storeName;
    @Column(name = "store_logo_url")
    private String storeLogo;
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;
    @Column(name = "cell_phone_number")
    private String cellPhoneNumber;
    @Column(name = "queue_full", nullable = false)
    private boolean queueFull;
    @Column(name = "open_for_delivery", nullable = false)
    private boolean openForDelivery;
    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "owners")
    @ElementCollection
    private List<String> owners = new ArrayList<String>();
    @Column(name = "account_manager")
    private String accountManager;
    @Column(name = "membership_code")
    private String memberShipCode;
    @Enumerated(EnumType.STRING)
    private MembershipType membershipType;
    @Enumerated(EnumType.STRING)
    private ChargeFrequency whenToCharge;
    @Column(name = "account_start_date")
    private LocalDate accountStartDate = LocalDate.now();
    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;
    @Column(name = "number_of_referrals")
    private int referrals;
    @Column(name = "number_of_transactions")
    private int transactions;
    @OneToOne(mappedBy = "store", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private HoursOfOperation storeHours;


}
