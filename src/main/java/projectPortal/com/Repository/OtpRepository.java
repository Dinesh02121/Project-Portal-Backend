package projectPortal.com.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectPortal.com.Entity.OtpEntity;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {

    Optional<OtpEntity> findByEmail(String email);
    Optional<OtpEntity> findByEmailAndOtp(String email, int otp);
}
