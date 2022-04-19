package com.howtech.posstorageapi.services.store;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.howtech.DTOs.CompanyDto;
import com.howtech.DTOs.HoursDto;
import com.howtech.DTOs.PayerDto;
import com.howtech.DTOs.StoreDto;
import com.howtech.exceptions.CustomerNotFoundException;
import com.howtech.exceptions.QueueFullException;
import com.howtech.exceptions.StoreNotFoundException;
import com.howtech.models.payment.StripeBank;
import com.howtech.models.store.AccountAddressInfo;
import com.howtech.models.store.AccountInfo;
import com.howtech.models.store.AddressInfo;
import com.howtech.models.store.BusinessInfo;
import com.howtech.models.store.Employee;
import com.howtech.models.store.HoursOfOperation;
import com.howtech.models.store.Product;
import com.howtech.models.store.Shipment;
import com.howtech.models.store.Store;
import com.howtech.models.store.StoreOrder;
import com.howtech.models.store.enums.MembershipType;
import com.howtech.posstorageapi.DTOs.ChargeDto;
import com.howtech.repositories.CustomerRepository;
import com.howtech.repositories.StoreRepository;
import com.howtech.services.S3.AmazonClient;
import com.howtech.services.payment.PayPalService;
import com.howtech.services.payment.PaymentService;
import com.howtech.services.payment.QuickBooksService;
import com.intuit.ipp.exception.FMSException;
import com.paypal.core.rest.PayPalRESTException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

/**
 *
 * @author Maurice Kelly
 * @apiNote this class implements the StoreService for inventory
 */

@Service
public class StoreService {

    Logger LOGGER = LoggerFactory.getLogger(StoreService.class);

    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;
    private final ValidateMembershipService validateMembershipService;
    private final PaymentService paymentService;
    private final PayPalService paypalService;
    public final QuickBooksService qboService;
    // private final AmazonClient amazonClient;

    public StoreService(StoreRepository storeRepository, CustomerRepository customerRepository,
                        ValidateMembershipService validateMembershipService, PaymentService paymentService,
                        PayPalService paypalService, QuickBooksService qboService /** , AmazonClient amazonClient **/
    ) {
        this.storeRepository = storeRepository;
        this.customerRepository = customerRepository;
        this.validateMembershipService = validateMembershipService;
        this.paymentService = paymentService;
        this.paypalService = paypalService;
        this.qboService = qboService;
        // this.amazonClient = amazonClient;
    }

    public Store createFromDto(StoreDto store, UserDetails userDetails) {

        LOGGER.info("Creating new Store");
        Store myStore = new Store();
        myStore.getOwners().add(userDetails.getUsername());
        myStore.setStoreName(store.getStoreName());
        myStore.setAccountManager(store.getAccountManager());
        myStore.setCellPhoneNumber(store.getCellphone());
        myStore.setPhoneNumber(store.getWorkphone());
        myStore.setQueueFull(false);
        myStore.setOpenForDelivery(false);
        myStore.setMembershipType(store.getMembershipType());
        myStore.setAccountStartDate(LocalDate.now());
        LOGGER.info("Adding address info");
        AddressInfo address = new AddressInfo();
        address.setCity(store.getStore_address_city());
        address.setCountry(store.getStore_address_country());
        address.setPostCode(store.getStore_address_postcode());
        address.setProvince(store.getStore_address_province());
        address.setStreet(store.getStore_address_street());
        myStore.setAddress(address);
        // set the address of the owner
        LOGGER.info("Setting Busuiness and Account info");
        AccountAddressInfo accountAddress = new AccountAddressInfo();
        BusinessInfo businessInfo = new BusinessInfo();
        businessInfo.setEIN(store.getBusiness_ein());
        businessInfo.setStoreName(store.getStoreName());
        myStore.setBusinessInfo(businessInfo);
        accountAddress.setCity(store.getAccount_address_city());
        accountAddress.setCountry(store.getAccount_address_country());
        accountAddress.setPostCode(store.getAccount_address_postcode());
        accountAddress.setProvince(store.getAccount_address_province());
        accountAddress.setStreet(store.getAccount_address_street());
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setAccountAddressInfo(accountAddress);
        myStore.setAccountInfo(accountInfo);
        myStore.setOwnerName(userDetails.getUsername());

        LOGGER.info("Validating membership code");
        if (store.getMembershipType().equals(MembershipType.BRONZE)) {
            if (validateMembershipService.verifyMembershipBronze(store.getMembershipCode())) {
                myStore.setMembershipType(MembershipType.BRONZE);
                myStore.setWhenToCharge(store.getWhenToCharge());
                myStore.setNextBillingDate();
                return storeRepository.save(myStore);
            } else {
                throw new AccessDeniedException("Membership code is wrong");
            }
        } else if (store.getMembershipType().equals(MembershipType.GOLD)) {
            if (validateMembershipService.verifyMembershipGold(store.getMembershipCode())) {
                myStore.setMembershipType(MembershipType.GOLD);
                myStore.setWhenToCharge(store.getWhenToCharge());
                myStore.setNextBillingDate();
                return storeRepository.save(myStore);
            } else {
                throw new AccessDeniedException("Membership code is wrong");
            }
        } else if (store.getMembershipType().equals(MembershipType.PLATINUM)) {
            if (validateMembershipService.verifyMembershipPlatinum(store.getMembershipCode())) {
                myStore.setMembershipType(MembershipType.PLATINUM);
                myStore.setWhenToCharge(store.getWhenToCharge());
                myStore.setNextBillingDate();
                return storeRepository.save(myStore);
            } else {
                throw new AccessDeniedException("Membership code is wrong");
            }
        } else {
            throw new AccessDeniedException("Membership does not exist");
        }
    }

    public Store addStoreHours(Long storeId, UserDetails userDetails, HoursDto hours) throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        if (myStore.getOwnerName().equals(userDetails.getUsername())) {
            HoursOfOperation storeHours = new HoursOfOperation(
                    LocalTime.of(hours.getMonOpenHour(), 0, 0, 0), LocalTime.of(hours.getTueOpenHour(), 0, 0, 0),
                    LocalTime.of(hours.getWedOpenHour(), 0, 0, 0),
                    LocalTime.of(hours.getThuOpenHour(), 0, 0, 0), LocalTime.of(hours.getFriOpenHour(), 0, 0, 0),
                    LocalTime.of(hours.getSatOpenHour(), 0, 0, 0),
                    LocalTime.of(hours.getSunOpenHour(), 0, 0, 0), LocalTime.of(hours.getMonCloseHour(), 0, 0, 0),
                    LocalTime.of(hours.getTueCloseHour(), 0, 0, 0),
                    LocalTime.of(hours.getWedCloseHour(), 0, 0, 0), LocalTime.of(hours.getThuCloseHour(), 0, 0, 0),
                    LocalTime.of(hours.getFriCloseHour(), 0, 0, 0),
                    LocalTime.of(hours.getSatCloseHour(), 0, 0, 0), LocalTime.of(hours.getSunCloseHour(), 0, 0, 0),
                    hours.isOpenForNewYearsEve(), LocalTime.of(hours.getNewYearsEveOpen(), 0, 0, 0),
                    LocalTime.of(hours.getNewYearsEveClose(), 0, 0, 0),
                    hours.isOpenForNewYears(), LocalTime.of(hours.getNewYearsOpen(), 0, 0, 0),
                    LocalTime.of(hours.getNewYearsClose(), 0, 0, 0),
                    hours.isOpenForIndependenceDay(), LocalTime.of(hours.getIndependenceOpen(), 0, 0, 0),
                    LocalTime.of(hours.getIndependenceClose(), 0, 0, 0),
                    hours.isOpenForMemorialDay(), LocalTime.of(hours.getMemorialDayOpen(), 0, 0, 0),
                    LocalTime.of(hours.getMemorialDayClose(), 0, 0, 0),
                    hours.isOpenForEaster(), LocalTime.of(hours.getEasterOpen(), 0, 0, 0),
                    LocalTime.of(hours.getEasterClose(), 0, 0, 0),
                    hours.isOpenForColumbusDay(), LocalTime.of(hours.getColumbusDayOpen(), 0, 0, 0),
                    LocalTime.of(hours.getColumbusDayClose(), 0, 0, 0),
                    hours.isOpenForThanksgiving(), LocalTime.of(hours.getThanksgivingOpen(), 0, 0, 0),
                    LocalTime.of(hours.getThanksgivingClose(), 0, 0, 0),
                    hours.isOpenForChristmasEve(), LocalTime.of(hours.getChristmasEveOpen(), 0, 0, 0),
                    LocalTime.of(hours.getChristmasEveClose(), 0, 0, 0),
                    hours.isOpenForChristmas(), LocalTime.of(hours.getChristmasOpen(), 0, 0, 0),
                    LocalTime.of(hours.getChristmasClose(), 0, 0, 0), myStore);
            myStore.setHoursOfOperation(storeHours);
            storeRepository.save(myStore);
            return myStore;
        } else {
            throw new AccessDeniedException("You don't own this store so you don't have permission to open it");
        }
    }

    public List<Store> getByOwnerName(UserDetails userDetails) {
        List<Store> stores = storeRepository.findAll();
        Predicate<Store> byOwner = store -> store.getOwnerName().equals(userDetails.getUsername());
        List<Store> myStores = stores.stream().filter(byOwner)
                .collect(Collectors.toList());
        return myStores;
    }

    public Store getById(Long storeId) throws StoreNotFoundException {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
    }

    public List<Store> getAll() {
        return storeRepository.findAll();
    }

    public String delete(Long storeId) {
        storeRepository.deleteById(storeId);
        return "Store with id " + storeId + " has been deleted";
    }

    public String deleteStores() {
        storeRepository.deleteAll();
        return "All stores have been deleted";
    }

    public List<String> getCustomerList(Long storeId, UserDetails userDetails) {
        Predicate<Store> byOwner = store -> store.getOwnerName().equals(userDetails.getUsername());
        List<Store> myStores = storeRepository
                .findAll()
                .parallelStream()
                .filter(byOwner)
                .collect(Collectors.toList());

        List<String> customerNames = new ArrayList<>();
        myStores
                .parallelStream()
                .forEach(store -> {
                    store.getStoreOrders()
                            .parallelStream()
                            .forEach(order -> {
                                customerNames.add(order.getCustomerName());
                            });
                });
        return customerNames;
    }

    public Set<Employee> getEmployees(UserDetails userDetails) {
        List<Store> stores = storeRepository.findAll();
        Predicate<Store> byOwner = store -> store.getOwnerName().equals(userDetails.getUsername());
        List<Store> myStores = stores.stream().filter(byOwner).collect(Collectors.toList());
        Set<Employee> myEmployees = new HashSet<>();
        myStores.stream().forEach(store -> {
            store.getEmployees().stream().forEach(ele -> {
                System.out.println(ele.getEmployeeId());
                myEmployees.add(ele);
            });
        });
        return myEmployees;
    }

    public Set<Product> getInventory(UserDetails userDetails) {
        List<Store> stores = storeRepository.findAll();
        Predicate<Store> byOwner = store -> store.getOwnerName().equals(userDetails.getUsername());
        List<Store> myStores = stores.stream().filter(byOwner).collect(Collectors.toList());
        Set<Product> myInventory = new HashSet<>();
        myStores.stream().forEach(store -> {
            store.getStoreInventory().stream().forEach(ele -> {
                myInventory.add(ele);
            });
        });
        return myInventory;
    }

    public Set<StoreOrder> getOrders(UserDetails userDetails) {
        List<Store> stores = storeRepository.findAll();
        Predicate<Store> byOwner = store -> store.getOwnerName().equals(userDetails.getUsername());
        List<Store> myStores = stores.stream().filter(byOwner).collect(Collectors.toList());
        Set<StoreOrder> orders = new HashSet<>();
        myStores.stream().forEach(store -> {
            store.getStoreOrders().stream().forEach(ele -> {
                orders.add(ele);
            });
        });
        return orders;
    }

    public Set<com.howtech.models.customer.Customer> getCustomers(UserDetails userDetails) {
        List<Store> stores = storeRepository.findAll();
        Predicate<Store> byOwner = store -> store.getOwnerName().equals(userDetails.getUsername());
        Set<Store> myStores = stores.stream().filter(byOwner).collect(Collectors.toSet());
        // TODO get all customers from all stores
        Set<com.howtech.models.customer.Customer> myCustomers = new HashSet<>();
        return myCustomers;// TODO: finish
        // doesn't need to be done yet do this after the customer side is done
        // use customer to store dto
        // use the customer id in the order
        // pull the customer out of customer repo
        // build the dto objects based off customer
    }

    public String referAStore(Long refererId, Long referedId, UserDetails userDetails) throws StoreNotFoundException {
        Store myStore = storeRepository.findById(refererId)
                .orElseThrow(() -> new StoreNotFoundException(refererId));
        if (myStore.getOwnerName().equals(userDetails.getUsername())) {
            // check to see if the other store has finished signing up first
            Store referedStore = storeRepository.findById(referedId)
                    .orElseThrow(() -> new StoreNotFoundException(referedId));
            if (referedStore.getMembershipType() != null) {
                myStore.addReferal(); // can be modified later to keep track of the store that was refered
                storeRepository.save(myStore);
            }
        } else {
            throw new AccessDeniedException("You don't own this store so you cannot refer another store");
        }
        return "Congradulations " + userDetails.getUsername() + " you have successfully refered store " + referedId;
    }

    public String openStore(Long storeId, UserDetails userDetails, Employee employee) throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        if (myStore.getOwnerName().equals(userDetails.getUsername())) {
            Set<Employee> storeEmployees = myStore.getEmployees();
            Predicate<Employee> byId = storeEmployee -> storeEmployee.getEmployeeId().equals(employee.getEmployeeId());
            Employee e = storeEmployees
                    .stream()
                    .filter(byId)
                    .collect(Collectors.toSet())
                    .iterator().next();
            if (!e.getCode().equals(employee.getCode())) {
                throw new AccessDeniedException("Your employee code is incorrect");
            }
            myStore.setOpenForDelivery(true);
        } else {
            throw new AccessDeniedException("You don't own this store so you don't have permission to open it");
        }
        return "Congragulations " + userDetails.getUsername() + " your store is now open for business!!!";
    }

    public String closeStore(Long storeId, UserDetails userDetails) throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));

        if (validStoreOwner(myStore, userDetails)) {
            myStore.setOpenForDelivery(false);
            storeRepository.save(myStore);
        } else {
            throw new AccessDeniedException("You don't own this store so you don't have permissions to access it");
        }
        return "Congragulations " + userDetails.getUsername() + " your store is now open for business!!!";
    }

    public Set<StoreOrder> getOrdersByStore(Long storeId, UserDetails userDetails) throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        Set<StoreOrder> orders;
        if (validStoreOwner(myStore, userDetails)) {
            orders = myStore.getStoreOrders();
        } else {
            throw new AccessDeniedException("You don't own this store so you don't have permissions to access it");
        }
        return orders;
    }

    private boolean validStoreOwner(Store myStore, UserDetails userDetails) {
        if (myStore.getOwnerName().equals(userDetails.getUsername()))
            return true;
        return false;
    }

    public Shipment fulfillOrder(Long storeId, Long orderId, Employee employee, UserDetails userDetails)
            throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        if (!myStore.getOwnerName().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("You do not have the auathority to access this resource");
        }
        Set<Employee> storeEmployees = myStore.getEmployees();
        Predicate<Employee> byId = storeEmployee -> storeEmployee.getEmployeeId().equals(employee.getEmployeeId());
        Employee e = storeEmployees
                .parallelStream()
                .filter(byId)
                .collect(Collectors.toSet())
                .iterator().next();
        if (!e.getCode().equals(employee.getCode())) {
            throw new AccessDeniedException("Your employee code is incorrect");
        }
        Shipment myShipment = new Shipment();
        myShipment.setEmployeeId(employee.getEmployeeId());
        myShipment.setOrderId(orderId);
        myShipment.setStore(myStore);
        myShipment.setStoreId(storeId);
        myStore.addShipment(myShipment);
        storeRepository.save(myStore);
        return myShipment;
        // TODO take the order and calculate the prices based off the cart items array
        // each item has an id from the stores inventory
        // TODO verify payment for the order
        // charge the customer
    }

    public StoreOrder orderFrom(Long storeId, Long customerId, StoreOrder order)
            throws CustomerNotFoundException, StoreNotFoundException, QueueFullException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        com.howtech.models.customer.Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        if (myStore.isQueueFull()) {
            throw new QueueFullException(storeId);
        }
        order.setCustomerName(customer.getUsername());
        myStore.addOrder(order);
        storeRepository.save(myStore);
        return order;
        // TODO go into storeOrder get the id of the inventory and use that to decrement
        // the amount of that inventory by the ammount requested
    }

    public Store addEmployee(Long storeId, Employee newEmployeeData, UserDetails userDetails)
            throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        Employee employee = new Employee();
        Set<Employee> employees = myStore.getEmployees();
        employee.setCode(newEmployeeData.getCode());
        employee.setEmail(newEmployeeData.getEmail());
        employee.setEmployeeUserId(newEmployeeData.getEmployeeUserId());
        employee.setFirstName(newEmployeeData.getFirstName());
        employee.setMiddleName(newEmployeeData.getMiddleName());
        employee.setLastName(newEmployeeData.getLastName());
        employee.setStore(myStore);
        employee.setEmployeePermissions(newEmployeeData.getEmployeePermissions());
        employees.add(employee);
        myStore.setEmployees(employees);
        return storeRepository.save(myStore);
    }

    public Employee getEmployee(Long storeId, Long employeeId, UserDetails userDetails) throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        Set<Employee> employees = myStore.getEmployees();
        Predicate<Employee> byId = employee -> employee.getEmployeeId().equals(employeeId);
        Set<Employee> employee = employees.stream().filter(byId).collect(Collectors.toSet());
        return employee.iterator().next();
    }

    public Store changeEmployeeData(Long storeId, Long employeeId, Employee newEmployeeData, UserDetails userDetails)
            throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        Set<Employee> employees = myStore.getEmployees();
        employees.stream()
                .forEach((employee) -> {
                    if (employee.getEmployeeId().equals(employeeId)) {
                        employee.setCode(newEmployeeData.getCode());
                        employee.setEmail(newEmployeeData.getEmail());
                        employee.setEmployeeUserId(newEmployeeData.getEmployeeUserId());
                        employee.setFirstName(newEmployeeData.getFirstName());
                        employee.setMiddleName(newEmployeeData.getMiddleName());
                        employee.setLastName(newEmployeeData.getLastName());
                    }
                });
        myStore.setEmployees(employees);
        return storeRepository.save(myStore);
    }

    public Set<Employee> getEmployees(Long storeId, UserDetails userDetails) throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        Set<Employee> employees = myStore.getEmployees();
        return employees;
    }

    public Store deleteEmployee(Long storeId, Long employeeId, UserDetails userDetails) throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        Set<Employee> employees = myStore.getEmployees();
        employees.stream()
                .forEach((employee) -> {
                    if (employee.getEmployeeId().equals(employeeId)) {
                        employees.remove(employee);
                    }
                });
        myStore.setEmployees(employees);
        return storeRepository.save(myStore);
    }

    public Store deleteEmployees(Long storeId, UserDetails userDetails) throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));

        myStore.setEmployees(new HashSet<>());
        return storeRepository.save(myStore);
    }

    public Set<Product> addInventoryItem(Long storeId, Product product, UserDetails userDetails)
            throws StoreNotFoundException {
        Product newProduct = new Product();
        newProduct.setImageURL(product.getImageURL());
        newProduct.setProductDescription(product.getProductDescription());
        newProduct.setProductName(product.getProductName());
        newProduct.setProductPrice(product.getProductPrice());
        newProduct.setProductQuantity(product.getProductQuantity());
        newProduct.setProductType(product.getProductType());
        newProduct.setDescriptions(product.getDescriptions());
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        if (!myStore.getOwnerName().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("You do not have the authority to access this resource");
        }
        newProduct.setStore(myStore);
        myStore.addInventory(newProduct);
        storeRepository.save(myStore);
        return myStore.getStoreInventory();
    }

    public Product getInventoryItem(Long storeId, Long inventoryId, UserDetails userDetails)
            throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        if (!myStore.getOwnerName().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("You do not have the authority to access this resource");
        }
        Set<Product> inventoryItems = myStore.getStoreInventory();
        Predicate<Product> byId = inventoryItem -> inventoryItem.getProductId().equals(inventoryId);
        Product product = inventoryItems.stream().filter(byId).collect(Collectors.toSet()).iterator().next();
        return product;
    }

    public Store changeInventoryItem(Long storeId, Long inventoryId, Product product, UserDetails userDetails)
            throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        if (!myStore.getOwnerName().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("You do not have the authority to access this resource");
        }

        myStore
                .getStoreInventory()
                .stream()
                .forEach((inventoryItem) -> {
                    if (inventoryItem.getProductId().equals(inventoryId)) {
                        inventoryItem.setDescriptions(product.getDescriptions());
                        inventoryItem.setImageURL(product.getImageURL());
                        inventoryItem.setProductBrand(product.getProductBrand());
                        inventoryItem.setProductCategory(product.getProductCategory());
                        inventoryItem.setProductDescription(product.getProductDescription());
                        inventoryItem.setProductName(product.getProductName());
                        inventoryItem.setProductPrice(product.getProductPrice());
                        inventoryItem.setProductQuantity(product.getProductQuantity());
                        inventoryItem.setProductSubCategory(product.getProductSubCategory());
                        inventoryItem.setProductType(product.getProductType());
                        inventoryItem.setProductUnit(product.getProductUnit());
                    }
                });
        return storeRepository.save(myStore);
    }

    public Set<Product> getInventory(Long storeId, UserDetails userDetails) throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        if (!myStore.getOwnerName().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("You do not have the authority to access this resource");
        }
        return myStore.getStoreInventory();
    }

    public String deleteInventoryItem(Long storeId, Long inventoryId, UserDetails userDetails)
            throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        if (!myStore.getOwnerName().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("You do not have the authority to access this resource");
        }

        myStore
                .getStoreInventory()
                .stream()
                .forEach((inventoryItem) -> {
                    if (inventoryItem.getProductId().equals(inventoryId)) {
                        myStore.getStoreInventory().remove(inventoryItem);
                    }
                });
        storeRepository.save(myStore);
        return "Item with store id " + storeId + " and inventory id " + inventoryId + " has been deleted";
    }

    public String deleteInventory(Long storeId, UserDetails userDetails) throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        myStore.setStoreInventory(null);
        storeRepository.save(myStore);
        return "All of store " + storeId + "'s inventory has been deleted";
    }

    public RedirectView connectToPaypal(Long storeId, HttpSession session, UserDetails userDetails)
            throws StoreNotFoundException, PayPalRESTException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        if (myStore.getOwnerName().equals(userDetails.getUsername())) {
            return paypalService.connectPaypal(session, storeId);
        } else {
            throw new AccessDeniedException(
                    "You don't own this store so you don't have permission to connect to paypal");
        }
    }

    public String paypalAuthRedirect(String authCode, HttpSession session, UserDetails userDetails)
            throws PayPalRESTException {
        paypalService.authorizeconnection(authCode, session);
        return "Success";
    }

    public void sendPaypalToStore(Long storeId, ChargeDto charge, UserDetails userDetails) throws IOException {
        paypalService.createPaypalPayout(charge.getAmount(), charge.getOrderId(), storeId);
    }

    public RedirectView connectToQuickbooks(Long storeId, HttpSession session, UserDetails userDetails)
            throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
        if (myStore.getOwnerName().equals(userDetails.getUsername())) {
            return qboService.connectToQuickbooks(session, storeId);
        } else {
            throw new AccessDeniedException(
                    "You don't own this store so you don't have permission to connect to quickbooks");
        }
    }

    public String callBackFromOAuth(String authCode, String state, String realmId, HttpSession session,
                                    UserDetails userDetails) {
        return qboService.callBackFromOAuth(authCode, state, realmId, session);
    }

    public String callQboVendorCreate(Long storeId, HttpSession session, CompanyDto company, UserDetails userDetails)
            throws JsonProcessingException, FMSException, ParseException, java.text.ParseException {
        return qboService.createQboVendor(storeId, company);
    }

    public String callBillingConcept(Long storeId, HttpSession session, ChargeDto chargedto, UserDetails userDetails)
            throws FMSException, ParseException, java.text.ParseException {
        return qboService.callBillingConcept(storeId, chargedto.getAmount());
    }

    public ResponseEntity<String> chargeStoreBankAccount(Long storeId, ChargeDto stripeCharge,
                                                         UserDetails userDetails) {
        return paymentService.createPaymentwithDefaultSource(storeId, stripeCharge);
    }

    public void createStoreBillingAccount(Long storeId, PayerDto acc, UserDetails userDetails) throws StripeException {
        Customer customer = paymentService.createPayer(storeId, acc);
        StripeBank stripeBank = paymentService.createBillingBankAccount(storeId);
        paymentService.createStripeCustomerBankAccount(stripeBank.getBankId(), acc);
        paymentService.validateStripeBankAccount(stripeBank.getBankId());
    }

    public void createStoreDepositAccount(Long storeId, CompanyDto acc, HttpServletRequest request,
                                          UserDetails userDetails) {
        try {
            paymentService.createVendorCompany(acc, request.getRemoteAddr(), storeId);
            paymentService.createVendorCompanyBankAccount(storeId, acc);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void payStoreDepositAccount(Long storeId, ChargeDto c, HttpServletRequest request, UserDetails userDetails) {
        try {
            paymentService.stripeDepositToBank(c.getAmount(), storeId, "py_1HXH3fDNHKH66w8gMT9OzMvi");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String uploadStoreLogo(Long storeId, MultipartFile file, UserDetails userDetails)
            throws StoreNotFoundException {
        String link = ""; // this.amazonClient.uploadFile(file);
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(userDetails.getUsername()));
        if (!authenticateStoreOwner(userDetails.getUsername(), myStore.getOwners())) {
            throw new AccessDeniedException("This is not your store");
        }
        // myStore.setStoreLogo(link);
        storeRepository.save(myStore);
        return link;
    }

    private boolean authenticateStoreOwner(String username, List<String> owners) {
        if (owners.contains(username)) {
            return true;
        } else {
            return false;
        }
    }

    public URL getStoreLogoUrl(Long storeId, UserDetails userDetails)
            throws StoreNotFoundException, MalformedURLException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(userDetails.getUsername()));
        if (!authenticateStoreOwner(userDetails.getUsername(), myStore.getOwners())) {
            throw new AccessDeniedException("This is not your store");
        }
        String link = myStore.getStoreLogo();
        return new URL(link); // amazonClient.getFileUrl(link);
    }

    public String deleteStoreImg(Long storeId, String fileUrl, UserDetails userDetails) throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(userDetails.getUsername()));
        if (!authenticateStoreOwner(userDetails.getUsername(), myStore.getOwners())) {
            throw new AccessDeniedException("This is not your store");
        }
        myStore.setStoreLogo(null);
        // return this.amazonClient.deleteFileFromS3Bucket(fileUrl);
        return "";
    }

    public String uploadInventoryPhoto(Long storeId, Long inventoryId, MultipartFile file, UserDetails userDetails)
            throws StoreNotFoundException {
        String link = ""; // this.amazonClient.uploadFile(file);
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(userDetails.getUsername()));
        if (!authenticateStoreOwner(userDetails.getUsername(), myStore.getOwners())) {
            throw new AccessDeniedException("This is not your store");
        }
        Set<Product> myInventory = myStore.getStoreInventory();
        myInventory
                .stream()
                .forEach((inventoryItem) -> {
                    if (inventoryId == inventoryItem.getProductId()) {
                        inventoryItem.setImageURL(link);
                    }
                });
        myStore.setStoreInventory(myInventory);
        storeRepository.save(myStore);
        return link;
    }

    public String getInventoryPhotoUrl(Long storeId, Long inventoryId, UserDetails userDetails)
            throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(userDetails.getUsername()));
        if (!authenticateStoreOwner(userDetails.getUsername(), myStore.getOwners())) {
            throw new AccessDeniedException("This is not your store");
        }
        Set<Product> myInventory = myStore.getStoreInventory();
        Predicate<Product> byId = product -> product.getProductId().equals(inventoryId);
        Product myProduct = myInventory
                .stream()
                .filter(byId)
                .collect(Collectors.toSet())
                .iterator()
                .next();
        return myProduct.getImageURL();

    }

    public String deleteProductImg(Long storeId, Long inventoryId, String fileUrl, UserDetails userDetails)
            throws StoreNotFoundException {
        Store myStore = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(userDetails.getUsername()));
        if (!authenticateStoreOwner(userDetails.getUsername(), myStore.getOwners())) {
            throw new AccessDeniedException("This is not your store");
        }
        Set<Product> myInventory = myStore.getStoreInventory();
        myInventory
                .stream()
                .forEach((inventoryItem) -> {
                    if (inventoryId == inventoryItem.getProductId()) {
                        inventoryItem.setImageURL(null);
                    }
                });
        myStore.setStoreInventory(myInventory);
        storeRepository.save(myStore);
        return ""; // this.amazonClient.deleteFileFromS3Bucket(fileUrl);
    }
}