package projectPortal.com.Entity;


import jakarta.persistence.*;
import lombok.Data;
import projectPortal.com.enums.ProjectStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "project")
@Data
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;
    private LocalDateTime lastEditedAt;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    @ManyToOne
    private StudentEntity createdBy;

    @ManyToOne
    private FacultyEntity assignedFaculty;

    private String college;
    private int progress;

    private String projectZipName;
    private String projectZipPath;

    private String extractedPath;


    private LocalDateTime submittedAt;
}
