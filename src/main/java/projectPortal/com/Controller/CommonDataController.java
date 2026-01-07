package projectPortal.com.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectPortal.com.DTO.CollegeDTO;
import projectPortal.com.DTO.FacultyDTO;
import projectPortal.com.Entity.CollegeEntity;
import projectPortal.com.Entity.FacultyEntity;
import projectPortal.com.Repository.CollegeRepository;
import projectPortal.com.Repository.FacultyRepository;
import projectPortal.com.enums.CollegeStatus;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")

public class CommonDataController {


    private final FacultyRepository facultyRepository;
    private final CollegeRepository collegeRepository;

    public CommonDataController(
            FacultyRepository facultyRepository, CollegeRepository collegeRepository) {
        this.facultyRepository = facultyRepository;
        this.collegeRepository = collegeRepository;
    }

    @GetMapping("/colleges")
    public List<CollegeDTO> getAllColleges() {
        return collegeRepository.findAllByStatus(CollegeStatus.APPROVED)
                .stream()
                .map(college -> new CollegeDTO(college.getCollegeId(), college.getCollegeName()))
                .collect(Collectors.toList());
    }

    @GetMapping("/colleges/{collegeId}/faculties")
    public List<FacultyDTO> getFacultiesByCollege(@PathVariable Long collegeId) {
        return facultyRepository.findByCollege_CollegeId(collegeId)
                .stream()
                .map(faculty -> new FacultyDTO(faculty.getFacultyId(), faculty.getFacultyName()))
                .collect(Collectors.toList());
    }

}