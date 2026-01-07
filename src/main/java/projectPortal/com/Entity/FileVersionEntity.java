package projectPortal.com.Entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name="file_version")
@Data
public class FileVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long versionId;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private ProjectEntity project;

    private String filePath;

    @Column
    private String content;

    @ManyToOne
    @JoinColumn(name = "edited_by")
    private StudentEntity editedBy;

    private LocalDateTime editedAt;


}
