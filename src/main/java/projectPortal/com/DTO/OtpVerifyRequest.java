package projectPortal.com.DTO;

import lombok.Data;

@Data
public class OtpVerifyRequest {
    private String email;
    private int otp;
}
