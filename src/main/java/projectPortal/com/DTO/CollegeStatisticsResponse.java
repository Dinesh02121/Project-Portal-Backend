package projectPortal.com.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CollegeStatisticsResponse {
    private long totalStudents;
    private long totalFaculty;
    private long totalProjects;
    private long pendingApprovals;
}
