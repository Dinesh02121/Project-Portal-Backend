package projectPortal.com.Controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import projectPortal.com.DTO.*;
import projectPortal.com.Entity.FacultyEntity;
import projectPortal.com.Entity.StudentEntity;
import projectPortal.com.Repository.CollegeAdminRepo;
import projectPortal.com.Service.CollegeMemberService;

import java.util.List;

@RestController
@RequestMapping("/college")
public class CollegeMemberDetails {

    @Autowired
    private CollegeMemberService collegeMemberService;
    @GetMapping("/test")
    public String test(){
        return "Test";
    }

    @GetMapping("/students")
    public ResponseEntity<List<StudentProfileResponse>> allRegisteredStudents(Authentication authentication) {
        try {
            String collegeEmail = authentication.getName();
            List<StudentProfileResponse> students = collegeMemberService.getAllRegisteredStudent(collegeEmail);
            return ResponseEntity.ok(students);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/students/recent")
    public ResponseEntity<List<StudentProfileResponse>> recentStudents(
            Authentication authentication,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            String collegeEmail = authentication.getName();
            List<StudentProfileResponse> students = collegeMemberService.getRecentStudents(collegeEmail, limit);
            return ResponseEntity.ok(students);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/faculty")
    public ResponseEntity<List<FacultyProfileResponse>> allFaculty(Authentication authentication) {
        try {
            String collegeEmail = authentication.getName();
            List<FacultyProfileResponse> faculty = collegeMemberService.getAllFaculty(collegeEmail);
            return ResponseEntity.ok(faculty);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/faculty/recent")
    public ResponseEntity<List<FacultyProfileResponse>> recentFaculty(
            Authentication authentication,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            String collegeEmail = authentication.getName();
            List<FacultyProfileResponse> faculty = collegeMemberService.getRecentFaculty(collegeEmail, limit);
            return ResponseEntity.ok(faculty);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<CollegeProfileResponse> getProfile(Authentication authentication) {
        try {
            String collegeEmail = authentication.getName();
            CollegeProfileResponse profile = collegeMemberService.getCollegeProfile(collegeEmail);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @PostMapping("/register")
    public String registerCollege(@RequestBody CollegeRegister collegeRegister,Authentication authentication){
       return collegeMemberService.registerCollege(authentication.getName(), collegeRegister);
    }

    @PutMapping("/profile")
    public ResponseEntity<CollegeProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody CollegeUpdateRequest updateRequest) {
        try {
            String collegeEmail = authentication.getName();
            CollegeProfileResponse updatedProfile = collegeMemberService.updateCollegeProfile(
                    collegeEmail,
                    updateRequest
            );
            return ResponseEntity.ok(updatedProfile);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<CollegeStatisticsResponse> getStatistics(Authentication authentication) {
        try {
            String collegeEmail = authentication.getName();
            CollegeStatisticsResponse statistics = collegeMemberService.getCollegeStatistics(collegeEmail);
            return ResponseEntity.ok(statistics);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

