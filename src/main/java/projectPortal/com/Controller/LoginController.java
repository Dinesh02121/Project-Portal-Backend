package projectPortal.com.Controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import projectPortal.com.DTO.LoginResponse;
import projectPortal.com.Entity.LoginCredentialEntity;
import projectPortal.com.Service.LoginService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    public LoginResponse loginFunction(
            @RequestBody LoginCredentialEntity loginCredential,
            HttpServletResponse response) {

        LoginResponse loginResponse = loginService.loginFunction(loginCredential);

        // Set httpOnly cookie with the token
        setAuthCookie(response, loginResponse.getToken());

        return loginResponse;
    }

    @PostMapping("/loginAdmin")
    public LoginResponse loginAdmin(
            @RequestBody LoginCredentialEntity loginCredential,
            HttpServletResponse response) {

        LoginResponse loginResponse = loginService.loginAdmin(loginCredential);

        // Set httpOnly cookie with the token
        setAuthCookie(response, loginResponse.getToken());

        return loginResponse;
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        // Clear the authToken cookie
        clearAuthCookie(response);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Logged out successfully");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyAuth(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", true);
            response.put("email", authentication.getName());

            // Get role from authorities
            String role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .orElse("UNKNOWN");

            response.put("role", role);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Helper method to set auth cookie
    private void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("authToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60); // 1 hour (match JWT expiration)

        response.addCookie(cookie);

        // Set SameSite attribute manually via header
        response.addHeader("Set-Cookie",
                String.format("authToken=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax",
                        token, 60 * 60));
    }

    // Helper method to clear auth cookie
    private void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("authToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately

        response.addCookie(cookie);

        // Also clear via header
        response.addHeader("Set-Cookie",
                "authToken=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
    }
}