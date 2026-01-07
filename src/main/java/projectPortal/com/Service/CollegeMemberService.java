package projectPortal.com.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectPortal.com.DTO.*;
import projectPortal.com.Entity.*;
import projectPortal.com.Repository.*;
import projectPortal.com.enums.CollegeStatus;
import projectPortal.com.enums.Role;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CollegeMemberService {
    @Autowired
    private EmailService emailService;
    private final CollegeAdminRepo collegeAdminRepo;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CollegeRepository collegeRepository;


    public CollegeMemberService(
            CollegeAdminRepo collegeAdminRepo,
            StudentRepository studentRepository,
            FacultyRepository facultyRepository,
            CollegeRepository collegeRepository
    ) {
        this.collegeAdminRepo = collegeAdminRepo;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.collegeRepository = collegeRepository;
    }
    private CollegeEntity getCollegeByAdminEmail(String email) {

        CollegeAdminEntity admin = collegeAdminRepo
                .findByUser_Email(email)
                .orElseThrow(() ->
                        new RuntimeException("College admin not found"));

        return collegeRepository
                .findByCollegeAdmin(admin)
                .orElseThrow(() ->
                        new RuntimeException("College not registered yet"));
    }

    public List<StudentProfileResponse> getAllRegisteredStudent(String collegeEmail) {

        CollegeEntity college = getCollegeByAdminEmail(collegeEmail);

        return studentRepository
                .findByCollege_CollegeId(college.getCollegeId())
                .stream()
                .map(student -> new StudentProfileResponse(
                        student.getStudentId(),
                        student.getStudentName(),
                        student.getRollNo(),
                        student.getBranch(),
                        student.getSemester(),
                        student.getUser().getEmail(),
                        college.getCollegeName()
                ))
                .toList();
    }

    public List<StudentProfileResponse> getRecentStudents(String collegeEmail, int limit) {

        CollegeEntity college = getCollegeByAdminEmail(collegeEmail);

        return studentRepository
                .findTop5ByCollege_CollegeIdOrderByCreatedAtDesc(college.getCollegeId())
                .stream()
                .map(student -> new StudentProfileResponse(
                        student.getStudentId(),
                        student.getStudentName(),
                        student.getRollNo(),
                        student.getBranch(),
                        student.getSemester(),
                        student.getUser().getEmail(),
                        college.getCollegeName()
                ))
                .toList();
    }


    public List<FacultyProfileResponse> getAllFaculty(String collegeEmail) {

        CollegeEntity college = getCollegeByAdminEmail(collegeEmail);

        return facultyRepository
                .findByCollege_CollegeId(college.getCollegeId())
                .stream()
                .map(faculty -> new FacultyProfileResponse(
                        faculty.getFacultyId(),
                        faculty.getFacultyName(),
                        faculty.getDepartment(),
                        faculty.getUser().getEmail(),
                        college.getCollegeName()
                ))
                .toList();
    }

    public List<FacultyProfileResponse> getRecentFaculty(String collegeEmail, int limit) {

        CollegeEntity college = getCollegeByAdminEmail(collegeEmail);

        return facultyRepository
                .findTop5ByCollege_CollegeIdOrderByCreatedAtDesc(college.getCollegeId())
                .stream()
                .map(faculty -> new FacultyProfileResponse(
                        faculty.getFacultyId(),
                        faculty.getFacultyName(),
                        faculty.getDepartment(),
                        faculty.getUser().getEmail(),
                        college.getCollegeName()
                ))
                .toList();
    }

    public CollegeProfileResponse getCollegeProfile(String collegeEmail) {

        CollegeEntity college = getCollegeByAdminEmail(collegeEmail);

        return new CollegeProfileResponse(
                college.getCollegeName(),
                college.getOfficialDomain(),
                collegeEmail,
                college.getAddress(),
                college.getCity(),
                college.getState(),
                college.getPincode(),
                college.getPhone(),
                college.getStatus(),
                college.getCollegeId()
        );
    }

    public String registerCollege(String collegeEmail, CollegeRegister request) {

        CollegeAdminEntity admin = collegeAdminRepo
                .findByUser_Email(collegeEmail)
                .orElseThrow(() ->
                        new RuntimeException("College admin not found"));

        if (collegeRepository.findByCollegeAdmin(admin).isPresent()) {
            throw new RuntimeException("College already registered");
        }

        CollegeEntity college = new CollegeEntity();
        college.setCollegeName(request.getCollegeName());
        college.setCity(request.getCity());
        college.setAddress(request.getAddress());
        college.setPincode(request.getPincode());
        college.setState(request.getState());
        college.setPhone(request.getPhone());
        college.setOfficialDomain(request.getOfficialDomain());
        college.setStatus(CollegeStatus.PENDING);
        college.setCollegeAdmin(admin);

        collegeRepository.save(college);




       String body= """
        Dear Admin,

        A new user has successfully registered on the application.

        User Registration Details:
        ---------------------------
      
        Email              : %s
        Role               : %s
        Registration Time  : %s
        Offical Domain     : %s

        The account is currently in PENDING status and requires your review
        and approval before full access is granted.

        Please log in to the Admin Dashboard to take the necessary action.

        Regards,
        Project Portal System
        (Automated Email – Do Not Reply)
        """.formatted(
                collegeEmail,
               Role.COLLEGE,
               LocalDateTime.now(),
               request.getOfficialDomain()
        );
       String subject="College Registration On Project Portal";
       emailService.sendEmail(subject,"dineshjangra0212@gmail.com",body);



        return "College registration submitted for approval";
    }

    @Transactional
    public CollegeProfileResponse updateCollegeProfile(
            String collegeEmail,
            CollegeUpdateRequest request
    ) {

        CollegeEntity college = getCollegeByAdminEmail(collegeEmail);

        college.setCollegeName(request.getCollegeName());
        college.setOfficialDomain(request.getOfficialDomain());
        college.setAddress(request.getAddress());
        college.setCity(request.getCity());
        college.setState(request.getState());
        college.setPincode(request.getPincode());
        college.setPhone(request.getPhone());
        college.setStatus(CollegeStatus.PENDING);

        return getCollegeProfile(collegeEmail);
    }

    public CollegeStatisticsResponse getCollegeStatistics(String collegeEmail) {

        CollegeEntity college = getCollegeByAdminEmail(collegeEmail);

        long students = studentRepository.countByCollege_CollegeId(college.getCollegeId());
        long faculty = facultyRepository.countByCollege_CollegeId(college.getCollegeId());

        return new CollegeStatisticsResponse(
                students,
                faculty,
                0,
                0
        );
    }
}
