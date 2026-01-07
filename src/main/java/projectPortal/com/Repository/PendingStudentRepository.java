package projectPortal.com.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectPortal.com.Entity.PendingStudentEntity;

import java.util.Optional;

public interface PendingStudentRepository
        extends JpaRepository<PendingStudentEntity, Long> {


    Optional<PendingStudentEntity> findByEmail(String email);

    Optional<PendingStudentEntity> findByEmailAndOtp(String email, int otp);
}
