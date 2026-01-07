package projectPortal.com.Repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectPortal.com.Entity.StudentEntity;
import projectPortal.com.Entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentEntity,Long> {
    List<StudentEntity> findByCollege_CollegeId(Long collegeId);
    Optional<StudentEntity> findByUser_Email(String email);

    List<StudentEntity> findTop5ByCollege_CollegeIdOrderByCreatedAtDesc(Long collegeId);
    long countByCollege_CollegeId(Long collegeId);

    Optional<StudentEntity> findByRollNo(String rollNumber);



}
