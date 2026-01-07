package projectPortal.com.Entity;


import jakarta.persistence.*;
import lombok.Data;

import projectPortal.com.enums.Role;

@Entity
@Table(name = "usersTable")
@Data
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled = true;
}
