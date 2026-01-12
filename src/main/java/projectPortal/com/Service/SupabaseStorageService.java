package projectPortal.com.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String bucketName;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Upload a file to Supabase Storage
     */
    public String uploadFile(String filePath, byte[] fileData, String contentType) {
        try {
            String url = String.format("%s/storage/v1/object/%s/%s", 
                    supabaseUrl, bucketName, filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("Content-Type", contentType != null ? contentType : "application/octet-stream");

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileData, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✓ File uploaded to Supabase: " + filePath);
                return filePath;
            } else {
                throw new RuntimeException("Failed to upload file to Supabase");
            }
        } catch (Exception e) {
            System.err.println("Error uploading to Supabase: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error uploading to Supabase: " + e.getMessage(), e);
        }
    }

    /**
     * Download a file from Supabase Storage
     */
    public byte[] downloadFile(String filePath) {
        try {
            String url = String.format("%s/storage/v1/object/%s/%s", 
                    supabaseUrl, bucketName, filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✓ File downloaded from Supabase: " + filePath);
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to download file from Supabase");
            }
        } catch (Exception e) {
            System.err.println("Error downloading from Supabase: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error downloading from Supabase: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a file from Supabase Storage
     */
    public void deleteFile(String filePath) {
        try {
            String url = String.format("%s/storage/v1/object/%s/%s", 
                    supabaseUrl, bucketName, filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
            System.out.println("✓ File deleted from Supabase: " + filePath);
        } catch (Exception e) {
            System.err.println("Warning: Could not delete file from Supabase: " + e.getMessage());
            // Don't throw - deletion failures shouldn't block the main operation
        }
    }

    /**
     * Check if file exists in Supabase
     */
    public boolean fileExists(String filePath) {
        try {
            String url = String.format("%s/storage/v1/object/%s/%s", 
                    supabaseUrl, bucketName, filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.HEAD,
                    requestEntity,
                    byte[].class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
