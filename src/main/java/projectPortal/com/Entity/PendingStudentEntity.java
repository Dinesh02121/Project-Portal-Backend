package projectPortal.com.Entity;

import jakarta.persistence.*;
import lombok.Data;
import projectPortal.com.enums.Semester;

import java.time.LocalDateTime;

@Entity
@Table(name = "pending_student")
@Data
public class PendingStudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentName;
    private String email;
    private String password;
    private String branch;
    private String rollNo;

    @Enumerated(EnumType.STRING)
    private Semester semester;

    @ManyToOne
    @JoinColumn(name = "college_id")
    private CollegeEntity college;

    private int otp;
    private LocalDateTime otpExpiry;
}
