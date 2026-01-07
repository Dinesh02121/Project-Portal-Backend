package projectPortal.com.Entity;


import jakarta.persistence.*;
import lombok.Data;

import projectPortal.com.enums.Semester;

import java.time.LocalDateTime;

@Entity
@Table(name = "studentsTable")
@Data
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String studentName;

    @Enumerated(EnumType.STRING)
    private Semester semester;

    @Column(nullable = false)
    private String branch;

    @Column(nullable = false)
    private String rollNo;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "college_id")
    private CollegeEntity college;
}
