package projectPortal.com.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectPortal.com.Entity.PendingPasswordResetEntity;

import java.util.Optional;

public interface PendingPasswordResetRepository
        extends JpaRepository<PendingPasswordResetEntity, Long> {

    Optional<PendingPasswordResetEntity> findByEmail(String email);

    Optional<PendingPasswordResetEntity> findByEmailAndOtp(String email, int otp);
}
