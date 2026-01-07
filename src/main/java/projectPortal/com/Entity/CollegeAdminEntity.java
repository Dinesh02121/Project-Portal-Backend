package projectPortal.com.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "college_admin")
@Data
public class CollegeAdminEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long collegeAdminId;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

}
