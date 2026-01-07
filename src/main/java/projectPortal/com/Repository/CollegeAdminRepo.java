package projectPortal.com.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectPortal.com.Entity.CollegeAdminEntity;
import projectPortal.com.Entity.CollegeEntity;
import projectPortal.com.Entity.UserEntity;

import java.util.Optional;

public interface CollegeAdminRepo extends JpaRepository<CollegeAdminEntity,Long> {
    Optional<CollegeAdminEntity> findByUser(UserEntity user);
    Optional<CollegeAdminEntity> findByUser_Email(String email);
    boolean existsByUser_Email(String email);
}
