package com.howtech.posstorageapi.services;

import com.howtech.posstorageapi.clients.S3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {

    private final Logger LOGGER = LoggerFactory.getLogger(StorageService.class);

    private final S3Client s3Client;

    public StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadStoreLogo(Long storeId, MultipartFile file, String username) {
        return s3Client.uploadStoreLogo(storeId, file, username);
    }
}
