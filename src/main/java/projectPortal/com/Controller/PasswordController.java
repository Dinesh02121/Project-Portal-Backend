package projectPortal.com.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import projectPortal.com.DTO.PasswordRest;
import projectPortal.com.Service.PasswordService;


@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordService passwordService;


    @PostMapping("/forgot")
    public ResponseEntity<String> sendOtp(@RequestParam String email) {
        return ResponseEntity.ok(passwordService.sendOrResendOtp(email));
    }

    @PostMapping("/resend")
    public ResponseEntity<String> resendOtp(@RequestParam String email) {
        return ResponseEntity.ok(passwordService.sendOrResendOtp(email));
    }


    @PostMapping("/reset")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordRest request) {
        return ResponseEntity.ok(passwordService.verifyOtpAndResetPassword(request));
    }
}
