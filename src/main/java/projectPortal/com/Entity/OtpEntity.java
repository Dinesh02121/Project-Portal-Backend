package projectPortal.com.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verification")
@Data
public class OtpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private int otp;

    @Column(nullable = false)
    private LocalDateTime expiryTime;
    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime lastSentTime;

    private boolean verified = false;
}
