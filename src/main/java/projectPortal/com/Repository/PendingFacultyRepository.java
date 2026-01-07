package projectPortal.com.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectPortal.com.Entity.PendingFacultyEntity;
import projectPortal.com.Entity.PendingStudentEntity;

import java.util.Optional;

public interface PendingFacultyRepository
        extends JpaRepository<PendingFacultyEntity, Long> {


    Optional<PendingFacultyEntity> findByEmail(String email);

    Optional<PendingFacultyEntity> findByEmailAndOtp(String email, int otp);
}
