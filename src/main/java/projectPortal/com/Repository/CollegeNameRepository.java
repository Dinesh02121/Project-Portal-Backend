package projectPortal.com.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectPortal.com.Entity.CollegeEntity;
import projectPortal.com.enums.CollegeStatus;

import java.util.Optional;

public interface CollegeNameRepository
        extends JpaRepository<CollegeEntity, Long> {

    Optional<CollegeEntity> findByCollegeNameAndStatus(
            String collegeName,
            CollegeStatus status
    );
    Optional<CollegeEntity> findByCollegeName(String collegeName);
}
