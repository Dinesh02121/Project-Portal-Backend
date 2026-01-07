package projectPortal.com.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailsDTO {
    private Long projectId;
    private String title;
    private String description;
    private String projectPath;
    private String studentName;
    private String studentEmail;
    private String status;
    private String college;
    private Integer progress;
    private String submittedAt;
}