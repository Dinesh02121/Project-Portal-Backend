package projectPortal.com.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectPortal.com.DTO.AdminStatisticsResponse;
import projectPortal.com.Entity.CollegeEntity;
import projectPortal.com.Entity.FacultyEntity;
import projectPortal.com.Entity.StudentEntity;
import projectPortal.com.Repository.*;
import projectPortal.com.enums.CollegeStatus;

import java.util.List;

@Service
public class AdminService {
    @Autowired
    private EmailService emailService;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CollegeNameRepository collegeNameRepository;
    private final CollegeRepository collegeRepository;

    public AdminService(StudentRepository studentRepository, FacultyRepository facultyRepository,
                        CollegeNameRepository collegeNameRepository,
                        CollegeRepository collegeRepository) {
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.collegeNameRepository = collegeNameRepository;

        this.collegeRepository = collegeRepository;
    }

    public List<StudentEntity> allStudent(){
        return studentRepository.findAll();
    }

    public List<FacultyEntity> allFaculty(){
        return facultyRepository.findAll();
    }

    public List<StudentEntity> collegeStudent(String collegeName){
        CollegeEntity college = collegeNameRepository
                .findByCollegeNameAndStatus(
                        collegeName,
                        CollegeStatus.APPROVED
                )
                .orElseThrow(() ->
                        new RuntimeException("College not registered or not approved")
                );

        Long collegeId = college.getCollegeId();
        return studentRepository.findByCollege_CollegeId(collegeId);
    }

    public List<FacultyEntity> collegeFaculty(String collegeName){
        CollegeEntity college = collegeNameRepository
                .findByCollegeNameAndStatus(
                        collegeName,
                        CollegeStatus.APPROVED
                )
                .orElseThrow(() ->
                        new RuntimeException("College not registered or not approved")
                );

        Long collegeId = college.getCollegeId();
        return facultyRepository.findByCollege_CollegeId(collegeId);
    }

    @Transactional
    public String approveCollege(String collegeName, CollegeStatus collegeStatus){
        CollegeEntity college = collegeNameRepository
                .findByCollegeName(collegeName)
                .orElseThrow(() ->
                        new RuntimeException("College not found: " + collegeName)
                );

        college.setStatus(collegeStatus);
        collegeNameRepository.save(college);
        String email=college.getCollegeAdmin().getUser().getEmail();
        String subject="College Status Update";

       String bodyAccepted="""
        Dear sir,

        Congratulations! 🎉

        Your account has been successfully approved by the administrator.
        Status : APPROVED

        You can now log in to the application and access all features
        available for your role.

        Login using your registered email and password.

        If you face any issues, please contact the support team.

        Best Regards,
        Project Portal Team
        (This is an automated message. Please do not reply.)
        """;



         String bodyRejected="""
         Dear Sir,
 
         Thank you for registering on the Project Portal.
 
         After reviewing your registration details, we regret to inform you
         that your account has not been approved at this time.
         
         Status : REJECTED
 
         Reason:
         You may update your information and register again if applicable.
         If you believe this decision was made in error, please contact
         the support team for further clarification.
 
         We appreciate your interest in our platform.
 
         Best Regards,
         Project Portal Team
         (This is an automated message. Please do not reply.)
         """;

         CollegeStatus collegeStatus1=CollegeStatus.APPROVED;

       if(college.getStatus().equals(collegeStatus1)) {
           emailService.sendEmail(subject, email, bodyAccepted);
       }
       else{
           emailService.sendEmail(subject,email,bodyRejected);
       }

        return collegeStatus + " Successfully";
    }

    public List<CollegeEntity> allCollege(){
        return collegeRepository.findAll();
    }

    public AdminStatisticsResponse findStatistic(){
        Long totalStudent = (long) studentRepository.findAll().size();
        Long totalFaculty = (long) facultyRepository.findAll().size();
        Long totalPending = collegeRepository.findByStatus(CollegeStatus.PENDING).stream().count();
        Long totalApproved = collegeRepository.findByStatus(CollegeStatus.APPROVED).stream().count();

        return new AdminStatisticsResponse(totalStudent, totalFaculty, totalApproved, totalPending);
    }
}