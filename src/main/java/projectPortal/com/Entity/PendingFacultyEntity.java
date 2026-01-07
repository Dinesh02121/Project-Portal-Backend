package projectPortal.com.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Table(name = "pending_faculty")
@Data
public class PendingFacultyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String facultyName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "college_id")
    private CollegeEntity college;

    @Column(nullable = false)
    private String department;

    private int otp;
    private LocalDateTime otpExpiry;
    private String email;
    String password;
}
