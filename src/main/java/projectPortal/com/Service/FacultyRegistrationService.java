package projectPortal.com.Service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import projectPortal.com.DTO.FacultyRegistrationRequest;
import projectPortal.com.DTO.OtpVerifyRequest;
import projectPortal.com.Entity.*;
import projectPortal.com.Repository.*;
import projectPortal.com.enums.CollegeStatus;
import projectPortal.com.enums.Role;
import projectPortal.com.otp.OtpGenerator;

import java.time.LocalDateTime;

@Service
public class FacultyRegistrationService {

    private final CollegeNameRepository collegeRepository;
    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final PendingFacultyRepository pendingFacultyRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpGenerator otpGenerator;

    public FacultyRegistrationService(
            CollegeNameRepository collegeRepository,
            FacultyRepository facultyRepository,
            UserRepository userRepository,
            PendingFacultyRepository pendingFacultyRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            OtpGenerator otpGenerator
    ) {
        this.collegeRepository = collegeRepository;
        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
        this.pendingFacultyRepository = pendingFacultyRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.otpGenerator = otpGenerator;
    }

    public String facultyRegistration(FacultyRegistrationRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        if (pendingFacultyRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("OTP already sent. Please verify.");
        }

        CollegeEntity college = collegeRepository
                .findByCollegeNameAndStatus(
                        request.getCollegeName(),
                        CollegeStatus.APPROVED)
                .orElseThrow(() ->
                        new RuntimeException("College not approved"));

        String emailDomain = request.getEmail()
                .substring(request.getEmail().indexOf("@") + 1);

        if (!emailDomain.equalsIgnoreCase(college.getOfficialDomain())) {
            throw new RuntimeException("Email domain does not match college");
        }

        int otp = otpGenerator.generateOtp();

        PendingFacultyEntity pending = new PendingFacultyEntity();
        pending.setFacultyName(request.getFacultyName());
        pending.setEmail(request.getEmail());
        pending.setPassword(passwordEncoder.encode(request.getPassword()));
        pending.setDepartment(request.getDepartment());
        pending.setCollege(college);
        pending.setOtp(otp);
        pending.setOtpExpiry(LocalDateTime.now().plusMinutes(10));

        pendingFacultyRepository.save(pending);
        String body="Hello "+ request.getFacultyName()+",\n" +
                "\n" +
                "We have received a request to verify your account.\n" +
                "\n" +
                "Your One-Time Password (OTP) is:\n" +
                "\n" +otp+
                "\n" +
                "\n" +
                "This OTP is valid for 10 minutes. Please do not share this code with anyone for security reasons.\n" +
                "\n" +
                "If you did not request this OTP, please ignore this email.\n" +
                "\n" +
                "Best regards,\n" +
                "Project Portal Team";

        emailService.sendEmail(
                "Faculty Registration OTP",
                request.getEmail(),
                body
        );

        return "OTP sent to your email";
    }

    public String verifyOtp(OtpVerifyRequest otpVerifyRequest) {

        PendingFacultyEntity pending = pendingFacultyRepository
                .findByEmailAndOtp(
                        otpVerifyRequest.getEmail(),
                        otpVerifyRequest.getOtp())
                .orElseThrow(() ->
                        new RuntimeException("Invalid OTP"));

        if (pending.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (userRepository.findByEmail(pending.getEmail()).isPresent()) {
            throw new RuntimeException("User already verified");
        }

        UserEntity user = new UserEntity();
        user.setEmail(pending.getEmail());
        user.setPassword(pending.getPassword());
        user.setRole(Role.FACULTY);
        user.setEnabled(true);
        userRepository.save(user);

        FacultyEntity faculty = new FacultyEntity();
        faculty.setFacultyName(pending.getFacultyName());
        faculty.setDepartment(pending.getDepartment());
        faculty.setCollege(pending.getCollege());
        faculty.setUser(user);
        faculty.setCreatedAt(LocalDateTime.now());
        faculty.setUpdatedAt(LocalDateTime.now());

        facultyRepository.save(faculty);

        pendingFacultyRepository.delete(pending);

        return "Faculty registration completed successfully";
    }
}
