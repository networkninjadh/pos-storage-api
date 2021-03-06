package com.howtech.posstorageapi.clients;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
/**
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
**/
@Component
public class S3Client {
    public String uploadStoreLogo(Long storeId, MultipartFile file, String username) {
        return "Success";
    }
/**
    private AmazonS3 s3client;

    private final Logger LOGGER = LoggerFactory.getLogger(S3Client.class);

    private String bucketName = System.getenv("AWS_BUCKET");

    private String accessKey = System.getenv("AWS_KEY");

    private String secretKey = System.getenv("AWS_SECRET");

    @PostConstruct
    private void initializeAmazon() {
        AWSCredentials credentials = new BasicAWSCredentials(
                accessKey,
                secretKey);

        this.s3client = AmazonS3ClientBuilder
                .standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.US_EAST_1)
                .build();
    }
    // us-east-1

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    private String generateFileName(MultipartFile multipartFile) {
        return new Date().getTime() + "-" + multipartFile.getOriginalFilename().replace(" ", "_");
    }

    private void uploadFileTos3bucket(String fileName, File file) {
        s3client.putObject(new PutObjectRequest(bucketName, fileName, file)); // filename is the key
    }

    public String uploadFile(MultipartFile multipartFile) {
        String fileName = "";
        try {
            File file = convertMultiPartToFile(multipartFile);
            fileName = generateFileName(multipartFile);
            uploadFileTos3bucket(fileName, file);
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOGGER.info("uploading a file");
        return fileName;
    }

    public File downloadFileFromS3(String filename) {
        File localFile = new File(filename);
        ObjectMetadata object = s3client.getObject(new GetObjectRequest(bucketName, filename), localFile);
        return localFile;
    }

    public URL getFileUrl(String filename) {
        return s3client.getUrl(bucketName, filename);
    }

    public String deleteFileFromS3Bucket(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        s3client.deleteObject(new DeleteObjectRequest(bucketName + "/", fileName));
        return "Successfully deleted";
    }
    **/


}
