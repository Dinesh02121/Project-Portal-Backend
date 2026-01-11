package projectPortal.com.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FacultyDashboardService {
    
    private final FacultyRepository facultyRepository;
    private final ProjectRepository projectRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public FacultyDashboardService(
            FacultyRepository facultyRepository, 
            ProjectRepository projectRepository) {
        this.facultyRepository = facultyRepository;
        this.projectRepository = projectRepository;
        this.restTemplate = new RestTemplate();
    }

    public FacultyProfile facultyProfile(String email) {
        FacultyEntity faculty = facultyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Not Found With This Email"));

        return new FacultyProfile(
                faculty.getFacultyName(), 
                faculty.getDepartment(), 
                email, 
                Role.FACULTY
        );
    }

    public FacultyDashboardSummary getSummary(String email) {
        FacultyEntity faculty = facultyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty Not Registered With This Email id"));

        long total = projectRepository.findByAssignedFaculty(faculty).size();
        long pending = projectRepository.findByAssignedFacultyAndStatus(
                faculty, ProjectStatus.FACULTY_REQUESTED).size();
        long accepted = projectRepository.findByAssignedFacultyAndStatus(
                faculty, ProjectStatus.FACULTY_ACCEPTED).size();
        long rejected = projectRepository.findByAssignedFacultyAndStatus(
                faculty, ProjectStatus.REJECTED).size();

        return new FacultyDashboardSummary(total, pending, accepted, rejected);
    }

    public ProjectDetailsDTO getProjectDetails(Long projectId, String facultyEmail) {
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
        FacultyEntity faculty = facultyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        return projectRepository.findByAssignedFacultyAndStatus(
                faculty,
                ProjectStatus.FACULTY_REQUESTED
        );
    }

    public String respondToProject(Long projectId, boolean accept, String email) {
        FacultyEntity faculty = facultyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized action");
        }

        project.setStatus(accept ? 
                ProjectStatus.FACULTY_ACCEPTED : ProjectStatus.REJECTED);
        projectRepository.save(project);

        return accept ? "Project accepted successfully" : "Project rejected";
    }

    public String setProgress(Long projectId, int progress) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        project.setProgress(progress);
        projectRepository.save(project);
        return "Progress Saved Successfully";
    }

    public List<FileInfo> getProjectFiles(Long projectId, String path, String email) {
        FacultyEntity faculty = facultyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        String projectPath = project.getExtractedPath();
        if (projectPath == null || projectPath.isEmpty()) {
            throw new RuntimeException("Project path not found");
        }

        Path basePath = Paths.get(projectPath);
        Path targetPath = (path == null || path.isEmpty()) ? 
                basePath : basePath.resolve(path);

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
        FacultyEntity faculty = facultyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        String projectPath = project.getExtractedPath();
        if (projectPath == null || projectPath.isEmpty()) {
            throw new RuntimeException("Project path not found");
        }

        Path basePath = Paths.get(projectPath);
        Path filePath = basePath.resolve(path);

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
        FacultyEntity faculty = facultyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        String projectPath = project.getExtractedPath();
        if (projectPath == null || projectPath.isEmpty()) {
            throw new RuntimeException("Project path not found");
        }

        Path basePath = Paths.get(projectPath);
        Path filePath = basePath.resolve(path);

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

    // NEW: AI Analysis Method
    public Map<String, Object> analyzeProject(Long projectId, String email) {
        // Verify faculty access
        FacultyEntity faculty = facultyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        // Get extracted path
        String extractedPath = project.getExtractedPath();
        if (extractedPath == null || extractedPath.isEmpty()) {
            throw new RuntimeException("Project files not found");
        }

        Path projectPath = Paths.get(extractedPath);
        if (!Files.exists(projectPath)) {
            throw new RuntimeException("Project directory not found on server");
        }

        // Read files from project
        Map<String, String> filesContent = readProjectFilesForAnalysis(projectPath);

        if (filesContent.isEmpty()) {
            throw new RuntimeException("No readable files found in project");
        }

        // Prepare request for Python AI service
        Map<String, Object> analysisRequest = new HashMap<>();
        analysisRequest.put("project_name", project.getTitle());
        analysisRequest.put("student_description", project.getDescription());
        analysisRequest.put("files_content", filesContent);

        // Call Python AI service
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(analysisRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServiceUrl + "/analyze-content",
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("AI service returned empty response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze project: " + e.getMessage());
        }
    }

   private Map<String, String> readProjectFilesForAnalysis(Path projectPath) {
    Map<String, String> filesContent = new HashMap<>();

    try {
      
        Set<String> skipDirs = Set.of(
                "node_modules", "venv", "__pycache__", "build", "dist",
                ".git", "target", "bin", "obj", ".next", ".nuxt", ".idea",
                "out", "logs", "temp", "tmp", ".mvn", ".gradle"
        );

        // Text file extensions - more comprehensive list
        Set<String> textExtensions = Set.of(
                ".java", ".js", ".jsx", ".ts", ".tsx", ".py", ".cpp", ".c", ".h", ".hpp",
                ".html", ".css", ".scss", ".sass", ".less",
                ".json", ".xml", ".yaml", ".yml", ".toml",
                ".md", ".txt", ".properties", ".env",
                ".sql", ".sh", ".bash", ".gradle", ".jsp", ".jspx",
                ".php", ".rb", ".go", ".rs", ".kt", ".swift"
        );

        // Priority files to always include (case-insensitive)
        Set<String> priorityFiles = Set.of(
                "readme.md", "readme.txt", "package.json", "pom.xml", 
                "build.gradle", "requirements.txt", "cargo.toml", 
                "go.mod", "application.properties", "application.yml"
        );

        // Collect all eligible files
        List<Path> allFiles = new ArrayList<>();
        
        Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    // Check if file is in a skip directory
                    String pathStr = path.toString();
                    for (String skipDir : skipDirs) {
                        if (pathStr.contains(File.separator + skipDir + File.separator) ||
                            pathStr.contains("/" + skipDir + "/")) {
                            return false;
                        }
                    }
                    return true;
                })
                .forEach(allFiles::add);

        System.out.println("Total files found (after filtering): " + allFiles.size());

        // Separate priority and regular files
        List<Path> priorityFilePaths = new ArrayList<>();
        List<Path> regularFilePaths = new ArrayList<>();

        for (Path file : allFiles) {
            String fileName = file.getFileName().toString().toLowerCase();
            String extension = "";
            
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0) {
                extension = fileName.substring(lastDot);
            }

            // Check if it's a priority file
            if (priorityFiles.contains(fileName)) {
                priorityFilePaths.add(file);
            } 
            // Check if it has a text extension
            else if (textExtensions.contains(extension)) {
                regularFilePaths.add(file);
            }
        }

        System.out.println("Priority files found: " + priorityFilePaths.size());
        System.out.println("Regular text files found: " + regularFilePaths.size());

        // Read priority files first (all of them)
        for (Path file : priorityFilePaths) {
            try {
                String content = Files.readString(file);
                String relativePath = projectPath.relativize(file).toString();

                // Limit content length
                if (content.length() > 5000) {
                    content = content.substring(0, 5000) + "\n... (truncated)";
                }

                filesContent.put(relativePath, content);
                System.out.println("Read priority file: " + relativePath);
            } catch (Exception e) {
                System.err.println("Could not read priority file: " + file.getFileName() + " - " + e.getMessage());
            }
        }

        // Read regular files (up to 15 more files)
        int regularFilesRead = 0;
        int maxRegularFiles = 15;
        
        for (Path file : regularFilePaths) {
            if (regularFilesRead >= maxRegularFiles) {
                break;
            }

            try {
                String content = Files.readString(file);
                String relativePath = projectPath.relativize(file).toString();

                // Limit content length
                if (content.length() > 3000) {
                    content = content.substring(0, 3000) + "\n... (truncated)";
                }

                filesContent.put(relativePath, content);
                regularFilesRead++;
                System.out.println("Read regular file: " + relativePath);
            } catch (Exception e) {
                System.err.println("Could not read file: " + file.getFileName() + " - " + e.getMessage());
            }
        }

        System.out.println("Total files read for analysis: " + filesContent.size());
        
        // If still no files, try to read ANY text file
        if (filesContent.isEmpty()) {
            System.out.println("No files with standard extensions found, trying all text files...");
            
            for (Path file : allFiles) {
                if (filesContent.size() >= 5) break; // At least get 5 files
                
                try {
                    // Try to read as text
                    String content = Files.readString(file);
                    String relativePath = projectPath.relativize(file).toString();
                    
                    if (content.length() > 3000) {
                        content = content.substring(0, 3000) + "\n... (truncated)";
                    }
                    
                    filesContent.put(relativePath, content);
                    System.out.println("Read fallback file: " + relativePath);
                } catch (Exception e) {
                    // Skip binary files or unreadable files
                    continue;
                }
            }
        }

    } catch (IOException e) {
        System.err.println("Error reading project files: " + e.getMessage());
        e.printStackTrace();
        throw new RuntimeException("Error reading project files: " + e.getMessage());
    }

    System.out.println("Final files content map size: " + filesContent.size());
    return filesContent;
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
