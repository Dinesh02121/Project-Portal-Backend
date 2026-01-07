package projectPortal.com.Repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectPortal.com.Entity.FacultyEntity;
import projectPortal.com.Entity.StudentEntity;
import projectPortal.com.Entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<FacultyEntity,Long> {
    Optional<FacultyEntity> findByUser_Email(String email);
    List<FacultyEntity> findByCollege_CollegeId(Long collegeId);
    long countByCollege_CollegeId(Long collegeId);

    List<FacultyEntity> findTop5ByCollege_CollegeIdOrderByCreatedAtDesc(Long collegeId);

    boolean existsByUser_Email(String email);


}
