package projectPortal.com.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import projectPortal.com.DTO.OtpVerifyRequest;
import projectPortal.com.DTO.StudentRegisterRequest;
import projectPortal.com.Entity.*;
import projectPortal.com.Repository.*;
import projectPortal.com.enums.CollegeStatus;
import projectPortal.com.enums.Role;
import projectPortal.com.otp.OtpGenerator;

import java.time.LocalDateTime;

@Service
public class StudentRegistrationService {

    @Autowired
    private EmailService emailService;


    private final CollegeNameRepository collegeRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PendingStudentRepository pendingStudentRepository;
    private final PasswordEncoder passwordEncoder;

    private final OtpGenerator otpGenerator;

    public StudentRegistrationService(
            CollegeNameRepository collegeRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            PendingStudentRepository pendingStudentRepository,
            PasswordEncoder passwordEncoder,

            OtpGenerator otpGenerator
    ) {
        this.collegeRepository = collegeRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.pendingStudentRepository = pendingStudentRepository;
        this.passwordEncoder = passwordEncoder;

        this.otpGenerator = otpGenerator;
    }


    public String registerStudent(StudentRegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        if (pendingStudentRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("OTP already sent. Please verify.");
        }

        CollegeEntity college = collegeRepository
                .findByCollegeNameAndStatus(
                        request.getCollegeName(),
                        CollegeStatus.APPROVED
                )
                .orElseThrow(() ->
                        new RuntimeException("College not approved or not found"));

        if (!request.getEmail().contains("@")) {
            throw new RuntimeException("Invalid email");
        }

        String emailDomain = request.getEmail()
                .substring(request.getEmail().indexOf("@") + 1);
        System.out.println(emailDomain);

        if (!emailDomain.equalsIgnoreCase(college.getOfficialDomain())) {
            throw new RuntimeException("Email domain does not match college");
        }

        int otp = otpGenerator.generateOtp();

        PendingStudentEntity pending = new PendingStudentEntity();
        pending.setStudentName(request.getStudentName());
        pending.setEmail(request.getEmail());
        pending.setPassword(passwordEncoder.encode(request.getPassword()));
        pending.setBranch(request.getBranch());
        pending.setRollNo(request.getRollNo());
        pending.setSemester(request.getSemester());
        pending.setCollege(college);
        pending.setOtp(otp);
        pending.setOtpExpiry(LocalDateTime.now().plusMinutes(10));

        pendingStudentRepository.save(pending);

        String body="Hello "+ request.getStudentName()+",\n" +
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
                "Student Registration OTP",
                request.getEmail(),
                body
        );

        return "OTP sent to your email";
    }


    public String verifyOtp(OtpVerifyRequest otpVerifyRequest) {

        PendingStudentEntity pending = pendingStudentRepository
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
        user.setRole(Role.STUDENT);
        user.setEnabled(true);
        userRepository.save(user);

        StudentEntity student = new StudentEntity();
        student.setStudentName(pending.getStudentName());
        student.setBranch(pending.getBranch());
        student.setSemester(pending.getSemester());
        student.setRollNo(pending.getRollNo());
        student.setCollege(pending.getCollege());
        student.setUser(user);
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());

        studentRepository.save(student);

        pendingStudentRepository.delete(pending);

        return "Student registration completed successfully";
    }
}
