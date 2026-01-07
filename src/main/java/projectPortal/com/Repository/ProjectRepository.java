package projectPortal.com.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import projectPortal.com.Entity.ProjectEntity;
import projectPortal.com.Entity.StudentEntity;
import projectPortal.com.Entity.FacultyEntity;
import projectPortal.com.enums.ProjectStatus;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {


    List<ProjectEntity>
    findTop5ByCreatedByOrderBySubmittedAtDesc(StudentEntity student);



    @Query("""
        SELECT COUNT(p) FROM ProjectEntity p
        WHERE p.createdBy = :student
        AND p.status ='IN_PROGRESS'
    """)
    long countActive(StudentEntity student);

    @Query("""
        SELECT COUNT(p) FROM ProjectEntity p
        WHERE p.createdBy = :student
        AND p.status IN ('SUBMITTED', 'FACULTY_REQUESTED')
    """)
    long countUnderReview(StudentEntity student);

    // Completed projects
    @Query("""
        SELECT COUNT(p) FROM ProjectEntity p
        WHERE p.createdBy = :student
        AND p.status =  ('FACULTY_ACCEPTED')
    """)
    long countCompleted(StudentEntity student);


    List<ProjectEntity> findByCreatedBy(StudentEntity createdBy);

    List<ProjectEntity> findByAssignedFaculty(FacultyEntity faculty);
    Optional<ProjectEntity> findByProjectId(Long projectId);

    List<ProjectEntity> findByAssignedFacultyAndStatus(
            FacultyEntity faculty,
            ProjectStatus status
    );


}
