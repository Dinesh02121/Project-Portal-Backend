package projectPortal.com.Service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import projectPortal.com.DTO.FacultyDashboardSummary;
import projectPortal.com.DTO.FacultyProfile;
import projectPortal.com.DTO.FileInfo;
import projectPortal.com.DTO.ProjectDetailsDTO;
import projectPortal.com.Entity.FacultyEntity;
import projectPortal.com.Entity.ProjectEntity;
import projectPortal.com.Repository.FacultyRepository;
import projectPortal.com.Repository.ProjectRepository;
import projectPortal.com.enums.ProjectStatus;
import projectPortal.com.enums.Role;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FacultyDashboardService {
    private final FacultyRepository facultyRepository;
    private final ProjectRepository projectRepository;

    public FacultyDashboardService(FacultyRepository facultyRepository, ProjectRepository projectRepository) {
        this.facultyRepository = facultyRepository;
        this.projectRepository = projectRepository;
    }

    public FacultyProfile facultyProfile(String email){
        FacultyEntity faculty = facultyRepository.findByUser_Email(email).orElseThrow(()->
                new RuntimeException("Not Found With This Email"));

        return new FacultyProfile(faculty.getFacultyName(), faculty.getDepartment(), email, Role.FACULTY);
    }

    public FacultyDashboardSummary getSummary(String email){
        FacultyEntity faculty = facultyRepository.findByUser_Email(email).orElseThrow(
                ()-> new RuntimeException("Faculty Not Registered With This Email id")
        );

        long total = projectRepository.findByAssignedFaculty(faculty).size();
        long pending = projectRepository.findByAssignedFacultyAndStatus(faculty, ProjectStatus.FACULTY_REQUESTED).size();
        long accepted = projectRepository.findByAssignedFacultyAndStatus(faculty, ProjectStatus.FACULTY_ACCEPTED).size();
        long rejected = projectRepository.findByAssignedFacultyAndStatus(faculty, ProjectStatus.REJECTED).size();

        return new FacultyDashboardSummary(total, pending, accepted, rejected);
    }

    public ProjectDetailsDTO getProjectDetails(Long projectId, String facultyEmail) {
        // Find the project
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));



        ProjectDetailsDTO details = new ProjectDetailsDTO();
        details.setProjectId(project.getProjectId());
        details.setTitle(project.getTitle());
        details.setDescription(project.getDescription());
        details.setProjectPath(project.getExtractedPath());
        details.setStatus(project.getStatus().toString());
        details.setCollege(project.getCollege());
        details.setProgress(project.getProgress());
        details.setSubmittedAt(project.getSubmittedAt() != null ?
                project.getSubmittedAt().toString() : null);

        return details;
    }



    public List<ProjectEntity> getRequestedProjects(String email) {
        FacultyEntity faculty = facultyRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        return projectRepository.findByAssignedFacultyAndStatus(
                faculty,
                ProjectStatus.FACULTY_REQUESTED
        );
    }

    public String respondToProject(Long projectId, boolean accept, String email) {
        FacultyEntity faculty = facultyRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getAssignedFaculty()
                .getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized action");
        }

        project.setStatus(
                accept
                        ? ProjectStatus.FACULTY_ACCEPTED
                        : ProjectStatus.REJECTED
        );

        projectRepository.save(project);

        return accept
                ? "Project accepted successfully"
                : "Project rejected";
    }

    public String setProgress(Long projectId, int progress){
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        project.setProgress(progress);
        projectRepository.save(project);
        return "Progress Saved Successfully";
    }

    // New methods for file operations
    public List<FileInfo> getProjectFiles(Long projectId, String path, String email) {
        FacultyEntity faculty = facultyRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify faculty is assigned to this project
        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        String projectPath = project.getExtractedPath();
        if (projectPath == null || projectPath.isEmpty()) {
            throw new RuntimeException("Project path not found");
        }

        Path basePath = Paths.get(projectPath);
        Path targetPath = (path == null || path.isEmpty())
                ? basePath
                : basePath.resolve(path);

        File directory = targetPath.toFile();
        if (!directory.exists() || !directory.isDirectory()) {
            throw new RuntimeException("Directory not found");
        }

        List<FileInfo> files = new ArrayList<>();
        File[] fileList = directory.listFiles();

        if (fileList != null) {
            for (File file : fileList) {
                try {
                    String relativePath = basePath.relativize(file.toPath()).toString();
                    FileInfo fileDTO = new FileInfo();
                    fileDTO.setName(file.getName());
                    fileDTO.setPath(relativePath);
                    fileDTO.setDirectory(file.isDirectory());

                    if (!file.isDirectory()) {
                        long sizeInBytes = file.length();
                        fileDTO.setSize(formatFileSize(sizeInBytes));
                    }

                    files.add(fileDTO);
                } catch (Exception e) {
                    continue;
                }
            }
        }

        return files;
    }

    public String getFileContent(Long projectId, String path, String email) {
        FacultyEntity faculty = facultyRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify faculty is assigned to this project
        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        String projectPath = project.getExtractedPath();
        if (projectPath == null || projectPath.isEmpty()) {
            throw new RuntimeException("Project path not found");
        }

        Path basePath = Paths.get(projectPath);
        Path filePath = basePath.resolve(path);

        // Security check: ensure the file is within the project directory
        if (!filePath.normalize().startsWith(basePath.normalize())) {
            throw new RuntimeException("Invalid file path");
        }

        File file = filePath.toFile();
        if (!file.exists() || file.isDirectory()) {
            throw new RuntimeException("File not found");
        }

        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + e.getMessage());
        }
    }

    public Resource downloadFile(Long projectId, String path, String email) {
        FacultyEntity faculty = facultyRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify faculty is assigned to this project
        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        String projectPath = project.getExtractedPath();
        if (projectPath == null || projectPath.isEmpty()) {
            throw new RuntimeException("Project path not found");
        }

        Path basePath = Paths.get(projectPath);
        Path filePath = basePath.resolve(path);

        // Security check: ensure the file is within the project directory
        if (!filePath.normalize().startsWith(basePath.normalize())) {
            throw new RuntimeException("Invalid file path");
        }

        File file = filePath.toFile();
        if (!file.exists() || file.isDirectory()) {
            throw new RuntimeException("File not found or is a directory");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File is not readable");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error downloading file: " + e.getMessage());
        }
    }

    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.2f KB", sizeInBytes / 1024.0);
        } else if (sizeInBytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", sizeInBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", sizeInBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
