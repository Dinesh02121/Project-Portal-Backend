package projectPortal.com.Service;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import projectPortal.com.DTO.OtpVerifyRequest;
import projectPortal.com.DTO.PasswordRest;
import projectPortal.com.Entity.LoginCredentialEntity;
import projectPortal.com.Entity.PendingPasswordResetEntity;
import projectPortal.com.Entity.UserEntity;
import projectPortal.com.Repository.PendingPasswordResetRepository;
import projectPortal.com.Repository.UserRepository;
import projectPortal.com.otp.OtpGenerator;

import java.time.LocalDateTime;

@Service
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpGenerator otpGenerator;
    private final PendingPasswordResetRepository pendingPasswordResetRepository;

    public PasswordService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            OtpGenerator otpGenerator,
            PendingPasswordResetRepository pendingPasswordResetRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.otpGenerator = otpGenerator;
        this.pendingPasswordResetRepository = pendingPasswordResetRepository;
    }


    public String sendOrResendOtp(String email) {

        userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PendingPasswordResetEntity pending =
                pendingPasswordResetRepository.findByEmail(email).orElse(null);


        if (pending != null) {
            if (pending.getLastSentTime().plusSeconds(60)
                    .isAfter(LocalDateTime.now())) {
                throw new RuntimeException("Please wait before requesting another OTP");
            }
            pendingPasswordResetRepository.delete(pending);
        }

        int otp = otpGenerator.generateOtp();

        PendingPasswordResetEntity newPending = new PendingPasswordResetEntity();
        newPending.setEmail(email);
        newPending.setOtp(otp);
        newPending.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        newPending.setAttempts(0);
        newPending.setLastSentTime(LocalDateTime.now());

        pendingPasswordResetRepository.save(newPending);

        String body = """
    Dear User,

    Your OTP for password reset is:

    %d

    This OTP is valid for 10 minutes.
    Please do not share it with anyone.

    Project Portal Team
    """.formatted(otp);

        emailService.sendEmail(
                "Password Reset OTP",
                email,
                body
        );

        return "OTP sent successfully";
    }


    public String verifyOtpAndResetPassword(PasswordRest request) {

        PendingPasswordResetEntity pending = pendingPasswordResetRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (pending.getExpiryTime().isBefore(LocalDateTime.now())) {
            pendingPasswordResetRepository.delete(pending);
            throw new RuntimeException("OTP expired");
        }


        if (pending.getAttempts() >= 3) {
            pendingPasswordResetRepository.delete(pending);
            throw new RuntimeException("Maximum OTP attempts exceeded");
        }


        if (pending.getOtp()!=(request.getOtp())) {
            pending.setAttempts(pending.getAttempts() + 1);
            pendingPasswordResetRepository.save(pending);
            throw new RuntimeException("Invalid OTP");
        }


        UserEntity user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);


        pendingPasswordResetRepository.delete(pending);

        return "Password reset successful";
    }
}
