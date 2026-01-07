package projectPortal.com.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import projectPortal.com.Entity.ProjectMemberEntity;
import projectPortal.com.Entity.StudentEntity;
import projectPortal.com.Entity.ProjectEntity;

import java.util.List;
public interface ProjectMemberRepository
        extends JpaRepository<ProjectMemberEntity, Long> {

    long countByStudent(StudentEntity student);

    List<ProjectMemberEntity> findByStudent(StudentEntity student);

    List<ProjectMemberEntity> findByProject(ProjectEntity project);
    @Modifying
    void deleteByProject(ProjectEntity project);


    long countByProject(ProjectEntity project);

}
