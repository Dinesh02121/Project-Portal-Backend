package projectPortal.com.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentDashboardSummary {

    private long active;
    private long review;
    private long completed;
    private long collaborations;
    private Long draft;
}
