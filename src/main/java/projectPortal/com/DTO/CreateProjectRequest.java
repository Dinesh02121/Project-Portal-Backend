package projectPortal.com.DTO;


import lombok.Data;

@Data
public class CreateProjectRequest {
    private String title;
    private String description;
    private Long collegeId;
    private Long facultyId;

}
