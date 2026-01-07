package projectPortal.com.DTO;


import lombok.Data;

@Data
public class PasswordRest {
    private String newPassword;
    private String email;
    private int otp;
}
