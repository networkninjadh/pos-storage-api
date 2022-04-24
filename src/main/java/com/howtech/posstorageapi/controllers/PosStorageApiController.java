package com.howtech.posstorageapi.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.net.URL;

@RestController
@RequestMapping("/storage")
public class PosStorageApiController {

    private final StoreService storeService;

    private final CustomerService customerService;

    private final DriverService driverService;

    private final HelpingHandService helpingHandService;

    public PosStorageApiController(StoreService storeService, CustomerService customerService,
       DriverService driverService, HelpingHandService helpingHandService) {
        this.storeService = storeService;
        this.driverService = driverService;
        this.customerService = customerService;
        this.helpingHandService = helpingHandService;

    }


    /**
     * Storage upload and download image url
     */

    /**
     * @param file a file to upload
     * @return the path to the file
     * @throws
     */
    @PostMapping("/store-profile/{store_id}")
    public String uploadStorageLogo(@PathVariable(name = "store_id") Long storeId,
            @RequestPart(value = "file") MultipartFile file, @AuthentictionPrincipal UserDetails userDetails)
    throws StoreNotFoundException  {
        return storeService.uploadStoreLogo(storeId, file, userDetails);
    }

    @GetMapping("/store-profile/{store_id}")
    public URL getStoreLogoUrl(@PathVariable(name = "store_id") Long storeId,
                               @AuthenticationPrincipal UserDetails userDetails) throws StoreNotFoundException, MalformedURLException {
        return storeService.getStoreLogoUrl(storeId, userDetails);
    }

    @DeleteMapping("/store-profile/delete/{store_id}")
    public String deleteStoreImg(@PathVariable(name = "store_id") Long storeId,
                                 @RequestPart(value = "url") String fileUrl, @AuthenticationPrincipal UserDetails userDetails)
            throws StoreNotFoundException {
        return storeService.deleteStoreImg(storeId, fileUrl, userDetails);
    }


}
