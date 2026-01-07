package projectPortal.com.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FacultyProfileResponse {
    private Long facultyId;
    private String facultyName;
    private String department;
    private String email;
    private String collegeName;


}
