package projectPortal.com.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${brevo.api.key}")
    private String apiKey;
    
    @Value("${brevo.api.url}")
    private String apiUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Async
    public void sendEmail(String subject, String toEmail, String body) {
        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            
            // Sender
            Map<String, String> sender = new HashMap<>();
            sender.put("email", fromEmail);
            sender.put("name", "Your App Name");
            requestBody.put("sender", sender);
            
            // Recipient
            Map<String, String> recipient = new HashMap<>();
            recipient.put("email", toEmail);
            requestBody.put("to", List.of(recipient));
            
            // Subject and body
            requestBody.put("subject", subject);
            requestBody.put("htmlContent", "<html><body><p>" + body + "</p></body></html>");
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            
            // Send request
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Email sent successfully to: " + toEmail);
            } else {
                System.err.println("Failed to send email: " + response.getBody());
            }
            
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
