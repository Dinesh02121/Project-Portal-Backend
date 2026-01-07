package projectPortal.com.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import projectPortal.com.DTO.LoginResponse;
import projectPortal.com.Entity.LoginCredentialEntity;
import projectPortal.com.Entity.UserEntity;
import projectPortal.com.Repository.UserRepository;
import projectPortal.com.Security.JwtUtil;


@Service
public class LoginService {
    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;
   final private UserRepository userRepository;
   final private PasswordEncoder passwordEncoder;
   final private JwtUtil jwtUtil;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse loginFunction(LoginCredentialEntity loginCredential){

        String userEmail = loginCredential.getEmail();

        UserEntity userEntity = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Invalid Email"));

        boolean isMatch = passwordEncoder.matches(
                loginCredential.getPassword(),
                userEntity.getPassword()
        );

        if (!isMatch) {
            throw new RuntimeException("Invalid Password");
        }

        String token = jwtUtil.generateToken(userEmail);

        return new LoginResponse(
                "Login Successful",
                token,
                userEntity.getRole().name(),
                userEntity.getEmail()
        );
    }


    public LoginResponse loginAdmin(LoginCredentialEntity loginCredential){



        if (!loginCredential.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new RuntimeException("Email Not Match");
        }

        if (!loginCredential.getPassword().equals(adminPassword)) {
            throw new RuntimeException("Wrong Password");
        }

        String token = jwtUtil.generateToken(adminEmail);

        return new LoginResponse(
                "Admin Login Successful",
                token,
                "ADMIN",
                adminEmail
        );
    }

}
