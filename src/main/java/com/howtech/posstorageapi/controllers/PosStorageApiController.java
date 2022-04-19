package com.howtech.posstorageapi.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/storage")
public class PosStorageApiController {




    /**
     * Storage upload and download image url
     */

    /**
     * @param file a file to upload
     * @return the path to the file
     * @throws
     */
    @PostMapping("/store-profile/{store_id}")
    public String uploadStorageLogo(@PathVariable (name = "store_id") Long storeId,
                                    @RequestPart(value = "file")MultipartFile file)) {

    }


}
