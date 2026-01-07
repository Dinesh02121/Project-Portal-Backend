package projectPortal.com.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import projectPortal.com.Entity.CollegeAdminEntity;
import projectPortal.com.Entity.CollegeEntity;
import projectPortal.com.enums.CollegeStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollegeRepository extends JpaRepository<CollegeEntity, Long> {
    Optional<CollegeEntity> findByCollegeAdmin(CollegeAdminEntity collegeAdmin);
    Optional<CollegeEntity> findByStatus(CollegeStatus collegeStatus);
    List<CollegeEntity> findAllByStatus(CollegeStatus collegeStatus);
}
