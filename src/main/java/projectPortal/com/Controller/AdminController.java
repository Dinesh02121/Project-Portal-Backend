package projectPortal.com.Controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import projectPortal.com.DTO.AdminStatisticsResponse;
import projectPortal.com.Entity.CollegeEntity;
import projectPortal.com.Entity.FacultyEntity;
import projectPortal.com.Entity.StudentEntity;
import projectPortal.com.Service.AdminService;
import projectPortal.com.enums.CollegeStatus;

import java.util.List;

@RestController
@RequestMapping("/auth/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;

    @GetMapping("/student")
    public List<StudentEntity>  allStudent(){
        return adminService.allStudent();
    }

    @GetMapping("studentData/{collegeName}")
    public List<StudentEntity> collegeAllStudent( @PathVariable String collegeName){
        return adminService.collegeStudent(collegeName);
    }

    @GetMapping("/faculty")
    public List<FacultyEntity>  allFaculty(){
        return adminService.allFaculty();

    }
    @GetMapping("/facultyData/{collegeName}")
    public List<FacultyEntity> collegeAllFaculty( @PathVariable String collegeName){

        return adminService.collegeFaculty(collegeName);

    }
    @PutMapping("approve/{collegeName}")
    public String approveCollege(@PathVariable String collegeName, @RequestBody CollegeStatus collegeStatus){
        return adminService.approveCollege(collegeName,collegeStatus);
    }

    @GetMapping("/colleges")
    public List<CollegeEntity> allColleges() {
        return adminService.allCollege();
    }

    @GetMapping("stats")
    public AdminStatisticsResponse statisticsResponse(){
        return adminService.findStatistic();
    }

}
