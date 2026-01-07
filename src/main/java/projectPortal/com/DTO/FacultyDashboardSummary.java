package projectPortal.com.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FacultyDashboardSummary {
    private long totalAssigned;
    private long pendingRequests;
    private long approved;
    private long rejected;
}
