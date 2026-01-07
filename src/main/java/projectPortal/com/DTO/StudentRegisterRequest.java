package projectPortal.com.DTO;


import lombok.*;
import projectPortal.com.enums.Semester;



@Data
public class StudentRegisterRequest {
    private String studentName;
    private String email;
    private String password;
    private String rollNo;
    private String branch;
    private Semester semester;
    private String collegeName;

}
