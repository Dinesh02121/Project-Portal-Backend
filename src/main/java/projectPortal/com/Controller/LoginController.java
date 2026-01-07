package projectPortal.com.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectPortal.com.DTO.LoginResponse;
import projectPortal.com.Entity.LoginCredentialEntity;
import projectPortal.com.Service.LoginService;

@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    public LoginResponse loginFunction(@RequestBody LoginCredentialEntity loginCredential){
        return loginService.loginFunction(loginCredential);
    }

    @PostMapping("/loginAdmin")
    public LoginResponse loginAdmin(@RequestBody LoginCredentialEntity loginCredential){
        return loginService.loginAdmin(loginCredential);
    }



}
