package projectPortal.com.Controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectPortal.com.DTO.*;
import projectPortal.com.Service.CollegeAdminRegistration;
import projectPortal.com.Service.FacultyRegistrationService;
import projectPortal.com.Service.StudentRegistrationService;

@RestController
@RequestMapping("/auth/registration")
public class RegistrationController {
    @Autowired
    private StudentRegistrationService studentRegistrationService;
    @Autowired
    private FacultyRegistrationService facultyRegistrationService;
    @Autowired
    private CollegeAdminRegistration collegeAdminRegistration;

    @PostMapping("/student")
    public String registerStudent(@RequestBody StudentRegisterRequest studentRegisterRequest){
        return studentRegistrationService.registerStudent(studentRegisterRequest);
    }



    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestBody OtpVerifyRequest otpVerifyRequest) {
        return studentRegistrationService.verifyOtp(otpVerifyRequest);
    }

    @PostMapping("/verify-otp-faculty")
    public String verifyOtpFaculty(@RequestBody OtpVerifyRequest otpVerifyRequest) {
        return facultyRegistrationService.verifyOtp(otpVerifyRequest);
    }


    @PostMapping("/faculty")
    public String registerFaculty(@RequestBody FacultyRegistrationRequest facultyRegistrationRequest){
        return facultyRegistrationService.facultyRegistration(facultyRegistrationRequest);
    }

    @PostMapping("collegeAdmin")
    public String registerCollegeAdmin(@RequestBody CollegeAdmin collegeAdmin){
        return collegeAdminRegistration.collegeAdminRegistration(collegeAdmin);
    }


}
