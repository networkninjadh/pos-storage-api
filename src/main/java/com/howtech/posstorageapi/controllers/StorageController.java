package com.howtech.posstorageapi.controllers;

import com.howtech.posstorageapi.services.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/storage")
public class StorageController {

    private final Logger LOGGER = LoggerFactory.getLogger(StorageController.class);

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/store-profile/{store_id}")
    public String uploadStoreLogo(@PathVariable(name = "store_id") Long storeId,
                                  @RequestHeader(name = "user-token") String username,
                                  @RequestPart(value ="file") MultipartFile file
                                 )
    {
        LOGGER.info("StorageController Uploading Store Logo");
        return storageService.uploadStoreLogo(storeId, file, username);
    }
}
