package projectPortal.com.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class StudentProjectResponse {

    private Long projectId;
    private String title;
    private String status;
    private int progress;
    private String facultyName;
    private boolean teamProject;
    private LocalDateTime createdAt;
}
