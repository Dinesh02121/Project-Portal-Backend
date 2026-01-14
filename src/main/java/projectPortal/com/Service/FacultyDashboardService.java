package projectPortal.com.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import projectPortal.com.DTO.FacultyDashboardSummary;
import projectPortal.com.DTO.FacultyProfile;
import projectPortal.com.DTO.FileInfo;
import projectPortal.com.DTO.ProjectDetailsDTO;
import projectPortal.com.Entity.FacultyEntity;
import projectPortal.com.Entity.ProjectEntity;
import projectPortal.com.Entity.StudentEntity;
import projectPortal.com.Repository.FacultyRepository;
import projectPortal.com.Repository.ProjectRepository;
import projectPortal.com.Repository.ProjectMemberRepository;
import projectPortal.com.enums.ProjectStatus;
import projectPortal.com.enums.Role;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FacultyDashboardService {
    
    private final FacultyRepository facultyRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RestTemplate restTemplate;

    @Autowired
    private SupabaseStorageService supabaseStorage;

    @Value("${ai.analysis.url:https://project-portal-review.onrender.com}")
    private String aiAnalysisUrl;

    public FacultyDashboardService(
            FacultyRepository facultyRepository, 
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            RestTemplateBuilder restTemplateBuilder) {
        this.facultyRepository = facultyRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(30))
            .setReadTimeout(Duration.ofSeconds(180))
            .build();
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
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectDetailsDTO details = new ProjectDetailsDTO();
        details.setProjectId(project.getProjectId());
        details.setTitle(project.getTitle());
        details.setDescription(project.getDescription());
        details.setProjectPath(project.getProjectZipPath());
        details.setStatus(project.getStatus().toString());
        details.setCollege(project.getCollege());
        details.setProgress(project.getProgress());
        details.setSubmittedAt(project.getSubmittedAt() != null ?
                project.getSubmittedAt().toString() : null);

        StudentEntity student = project.getCreatedBy();
        if (student != null) {
            details.setStudentName(student.getStudentName());
            details.setStudentEmail(student.getUser().getEmail());
        }

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

    public List<ProjectEntity> getAllAssignedProjects(String email) {
        FacultyEntity faculty = facultyRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        return projectRepository.findByAssignedFaculty(faculty);
    }

    public List<ProjectEntity> getAcceptedProjects(String email) {
        FacultyEntity faculty = facultyRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        return projectRepository.findByAssignedFacultyAndStatus(
                faculty,
                ProjectStatus.FACULTY_ACCEPTED
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

    public String runAIAnalysis(Long projectId, String facultyEmail) {
        try {
            FacultyEntity faculty = facultyRepository
                    .findByUser_Email(facultyEmail)
                    .orElseThrow(() -> new RuntimeException("Faculty not found"));

            ProjectEntity project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
                throw new RuntimeException("Unauthorized: You are not assigned to this project");
            }

            String zipPath = project.getProjectZipPath();
            if (zipPath == null || zipPath.isEmpty()) {
                throw new RuntimeException("Project ZIP not found in storage");
            }

            byte[] zipBytes = supabaseStorage.downloadFile(zipPath);

            boolean serviceReady = wakeUpAIService(3);
            
            String analysisResult = sendAnalysisRequest(
                project.getTitle(), 
                project.getDescription(), 
                zipBytes
            );

            return analysisResult;

        } catch (ResourceAccessException e) {
            throw new RuntimeException("AI service is not responding. It might be waking up. Please wait 1-2 minutes and try again.");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("AI service error: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("AI Analysis failed: " + e.getMessage());
        }
    }

    private boolean wakeUpAIService(int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                ResponseEntity<String> response = restTemplate.exchange(
                    aiAnalysisUrl + "/warmup",
                    HttpMethod.GET,
                    entity,
                    String.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    Thread.sleep(2000);
                    return true;
                }
                
            } catch (ResourceAccessException e) {
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(10000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return false;
    }

    private String sendAnalysisRequest(String projectName, String description, byte[] zipBytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("project_name", projectName != null ? projectName : "Untitled Project");
        body.add("student_description", description != null ? description : "No description provided");
        
        ByteArrayResource fileResource = new ByteArrayResource(zipBytes) {
            @Override
            public String getFilename() {
                return "project.zip";
            }
        };
        body.add("project_zip", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = 
            new HttpEntity<>(body, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            aiAnalysisUrl + "/analyze", 
            requestEntity, 
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("Analysis failed with status: " + response.getStatusCode());
        }
    }

    public List<FileInfo> getProjectFiles(Long projectId, String path, String email) {
        FacultyEntity faculty = facultyRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        try {
            String zipPath = project.getProjectZipPath();
            if (zipPath == null || zipPath.isEmpty()) {
                throw new RuntimeException("Project ZIP not found in storage");
            }

            byte[] zipBytes = supabaseStorage.downloadFile(zipPath);

            return listFilesInZip(zipBytes, path);

        } catch (Exception e) {
            throw new RuntimeException("Error reading project files: " + e.getMessage());
        }
    }

    public String getFileContent(Long projectId, String path, String email) {
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("File path is required");
        }

        FacultyEntity faculty = facultyRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        try {
            String zipPath = project.getProjectZipPath();
            if (zipPath == null || zipPath.isEmpty()) {
                throw new RuntimeException("Project ZIP not found in storage");
            }

            byte[] zipBytes = supabaseStorage.downloadFile(zipPath);

            return extractFileFromZip(zipBytes, path);

        } catch (Exception e) {
            throw new RuntimeException("Error reading file: " + e.getMessage());
        }
    }

    public Resource downloadFile(Long projectId, String path, String email) {
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("File path is required");
        }

        FacultyEntity faculty = facultyRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        try {
            String zipPath = project.getProjectZipPath();
            if (zipPath == null || zipPath.isEmpty()) {
                throw new RuntimeException("Project ZIP not found in storage");
            }

            byte[] zipBytes = supabaseStorage.downloadFile(zipPath);

            byte[] fileBytes = extractFileBytesFromZip(zipBytes, path);
            
            return new ByteArrayResource(fileBytes);

        } catch (Exception e) {
            throw new RuntimeException("Error downloading file: " + e.getMessage());
        }
    }

    private List<FileInfo> listFilesInZip(byte[] zipBytes, String prefix) throws IOException {
        List<FileInfo> fileInfos = new ArrayList<>();
        Set<String> addedDirectories = new HashSet<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.contains("__MACOSX") || entryName.contains(".DS_Store")) {
                    continue;
                }

                if (prefix != null && !prefix.isEmpty()) {
                    String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
                    
                    if (!entryName.startsWith(normalizedPrefix) && !entryName.equals(prefix)) {
                        continue;
                    }
                    
                    entryName = entryName.substring(normalizedPrefix.length());
                }

                if (entryName.isEmpty() || entryName.equals("/")) {
                    continue;
                }

                boolean isDirectory = entryName.endsWith("/") || entry.isDirectory();
                if (entryName.endsWith("/")) {
                    entryName = entryName.substring(0, entryName.length() - 1);
                }

                if (entryName.isEmpty()) {
                    continue;
                }

                String[] parts = entryName.split("/");
                String firstName = parts[0];

                if (parts.length > 1) {
                    if (!addedDirectories.contains(firstName)) {
                        String dirPath = prefix != null && !prefix.isEmpty() ? 
                                prefix + "/" + firstName : firstName;
                        fileInfos.add(new FileInfo(firstName, dirPath, true, "-"));
                        addedDirectories.add(firstName);
                    }
                } else {
                    if (isDirectory) {
                        if (!addedDirectories.contains(firstName)) {
                            String dirPath = prefix != null && !prefix.isEmpty() ? 
                                    prefix + "/" + firstName : firstName;
                            fileInfos.add(new FileInfo(firstName, dirPath, true, "-"));
                            addedDirectories.add(firstName);
                        }
                    } else {
                        if (!addedDirectories.contains(firstName)) {
                            String filePath = prefix != null && !prefix.isEmpty() ? 
                                    prefix + "/" + firstName : firstName;
                            String size = formatFileSize(entry.getSize());
                            fileInfos.add(new FileInfo(firstName, filePath, false, size));
                        }
                    }
                }

                zis.closeEntry();
            }
        }

        fileInfos.sort((a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        return fileInfos;
    }

    private String extractFileFromZip(byte[] zipBytes, String filePath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(filePath)) {
                    if (entry.isDirectory()) {
                        throw new RuntimeException("Cannot read content of a directory");
                    }
                    byte[] fileBytes = zis.readAllBytes();
                    return new String(fileBytes, StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }
        }
        throw new RuntimeException("File not found in project: " + filePath);
    }

    private byte[] extractFileBytesFromZip(byte[] zipBytes, String filePath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(filePath)) {
                    if (entry.isDirectory()) {
                        throw new RuntimeException("Cannot download folders. Only individual files can be downloaded.");
                    }
                    return zis.readAllBytes();
                }
                zis.closeEntry();
            }
        }
        throw new RuntimeException("File not found in project: " + filePath);
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
