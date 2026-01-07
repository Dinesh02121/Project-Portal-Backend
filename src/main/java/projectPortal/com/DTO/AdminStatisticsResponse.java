package projectPortal.com.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminStatisticsResponse {
    private Long totalStudent;
    private Long totalFaculty;
    private Long totalApproved;
    private Long totalPending;
}
