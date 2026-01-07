package projectPortal.com.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import projectPortal.com.enums.Semester;

@Data
@AllArgsConstructor
public class StudentProfileResponse {
    private Long studentId;
    private String studentName;
    private String rollNo;
    private String branch;
    private Semester semester;
    private String email;
    private String collegeName;



}
