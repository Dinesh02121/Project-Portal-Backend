package projectPortal.com.Entity;


import jakarta.persistence.Entity;
import lombok.Data;
@Data
public class LoginCredentialEntity {
    private String email;
    private String password;
}
