package projectPortal.com.Entity;

import jakarta.persistence.*;
import lombok.Data;
import projectPortal.com.enums.MemberRole;

@Entity
@Table(
        name = "project_member",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"project_id", "student_id"})
        }
)
@Data
public class ProjectMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Project reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    // 🔗 Student reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    // 👤 Role inside project
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;   // LEADER, MEMBER
}
