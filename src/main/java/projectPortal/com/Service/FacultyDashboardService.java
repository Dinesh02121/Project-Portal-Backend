package projectPortal.com.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FacultyDashboardService {
    
    @Autowired
    private SupabaseStorageService supabaseStorage;
    
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

        try {
            // Download ZIP from Supabase
            String zipPath = project.getProjectZipPath();
            byte[] zipBytes = supabaseStorage.downloadFile(zipPath);

            // List files in ZIP
            return listFilesInZip(zipBytes, path);

        } catch (Exception e) {
            System.err.println("Error reading project files: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error reading project files: " + e.getMessage());
        }
    }

    public String getFileContent(Long projectId, String path, String email) {
        FacultyEntity faculty = facultyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        try {
            // Download ZIP from Supabase
            String zipPath = project.getProjectZipPath();
            byte[] zipBytes = supabaseStorage.downloadFile(zipPath);

            // Extract specific file content from ZIP
            return extractFileFromZip(zipBytes, path);

        } catch (Exception e) {
            System.err.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
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

        try {
            // Download ZIP from Supabase
            String zipPath = project.getProjectZipPath();
            byte[] zipBytes = supabaseStorage.downloadFile(zipPath);

            // Extract specific file from ZIP
            byte[] fileBytes = extractFileBytesFromZip(zipBytes, path);
            
            return new ByteArrayResource(fileBytes);

        } catch (Exception e) {
            System.err.println("Error downloading file: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error downloading file: " + e.getMessage());
        }
    }

    // AI Analysis Method
    public Map<String, Object> analyzeProject(Long projectId, String email) {
        System.out.println("=== PROJECT ANALYSIS DEBUG ===");
        System.out.println("Project ID: " + projectId);
        
        // Verify faculty access
        FacultyEntity faculty = facultyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        System.out.println("Project Title: " + project.getTitle());
        System.out.println("ZIP Path in Supabase: " + project.getProjectZipPath());

        try {
            // Download ZIP from Supabase
            String zipPath = project.getProjectZipPath();
            byte[] zipBytes = supabaseStorage.downloadFile(zipPath);
            
            System.out.println("✓ Downloaded ZIP from Supabase, size: " + zipBytes.length + " bytes");

            // Extract and read files from ZIP
            Map<String, String> filesContent = extractAndReadZipFiles(zipBytes);

            if (filesContent.isEmpty()) {
                throw new RuntimeException("No readable files found in project ZIP");
            }

            System.out.println("✓ Extracted " + filesContent.size() + " files from ZIP");
            System.out.println("Files: " + filesContent.keySet());

            // Prepare request for Python AI service
            Map<String, Object> analysisRequest = new HashMap<>();
            analysisRequest.put("project_name", project.getTitle());
            analysisRequest.put("student_description", project.getDescription());
            analysisRequest.put("files_content", filesContent);

            System.out.println("Calling AI service at: " + aiServiceUrl + "/analyze-content");

            // Call Python AI service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(analysisRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiServiceUrl + "/analyze-content",
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                System.out.println("✓ AI Analysis successful!");
                return response.getBody();
            } else {
                throw new RuntimeException("AI service returned empty response");
            }
        } catch (Exception e) {
            System.err.println("AI Service Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to analyze project: " + e.getMessage());
        }
    }

    // Helper method to extract and read files from ZIP
    private Map<String, String> extractAndReadZipFiles(byte[] zipBytes) throws IOException {
        Map<String, String> filesContent = new HashMap<>();

        // Skip directories
        Set<String> skipDirs = Set.of(
                "node_modules", "venv", "__pycache__", "build", "dist",
                ".git", "target", "bin", "obj", ".next", ".nuxt", ".idea",
                "out", "logs", "temp", "tmp", ".mvn", ".gradle", "classes"
        );

        // Text file extensions
        Set<String> textExtensions = Set.of(
                ".java", ".js", ".jsx", ".ts", ".tsx", ".py", ".cpp", ".c", ".h", ".hpp",
                ".html", ".css", ".scss", ".sass", ".less",
                ".json", ".xml", ".yaml", ".yml", ".toml",
                ".md", ".txt", ".properties", ".env",
                ".sql", ".sh", ".bash", ".gradle", ".jsp", ".jspx",
                ".php", ".rb", ".go", ".rs", ".kt", ".swift"
        );

        // Priority files
        Set<String> priorityFilesLower = Set.of(
                "readme.md", "readme.txt", "readme",
                "package.json", "pom.xml", "build.gradle", "settings.gradle",
                "requirements.txt", "cargo.toml", "go.mod",
                "application.properties", "application.yml", "application.yaml"
        );

        List<ZipFileEntry> allFiles = new ArrayList<>();
        List<ZipFileEntry> priorityFiles = new ArrayList<>();

        // First pass: collect all entries
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String entryName = entry.getName();
                    
                    // Check if in skip directory
                    boolean inSkipDir = false;
                    for (String skipDir : skipDirs) {
                        if (entryName.contains("/" + skipDir + "/") || 
                            entryName.startsWith(skipDir + "/")) {
                            inSkipDir = true;
                            break;
                        }
                    }

                    if (inSkipDir) {
                        zis.closeEntry();
                        continue;
                    }

                    String fileName = entryName.substring(entryName.lastIndexOf('/') + 1).toLowerCase();
                    String extension = "";
                    int lastDot = fileName.lastIndexOf('.');
                    if (lastDot > 0) {
                        extension = fileName.substring(lastDot).toLowerCase();
                    }

                    // Read file content
                    byte[] fileBytes = zis.readAllBytes();
                    
                    // Check if priority file
                    if (priorityFilesLower.contains(fileName)) {
                        priorityFiles.add(new ZipFileEntry(entryName, fileBytes));
                    } 
                    // Check if has text extension
                    else if (textExtensions.contains(extension)) {
                        allFiles.add(new ZipFileEntry(entryName, fileBytes));
                    }
                }
                zis.closeEntry();
            }
        }

        System.out.println("Found " + priorityFiles.size() + " priority files");
        System.out.println("Found " + allFiles.size() + " regular text files");

        // Read priority files first
        for (ZipFileEntry fileEntry : priorityFiles) {
            try {
                String content = new String(fileEntry.content, StandardCharsets.UTF_8);
                
                if (content.length() > 5000) {
                    content = content.substring(0, 5000) + "\n... (truncated)";
                }

                filesContent.put(fileEntry.name, content);
                System.out.println("✓ Read priority file: " + fileEntry.name);
            } catch (Exception e) {
                System.err.println("✗ Failed to read priority file: " + fileEntry.name);
            }
        }

        // Read up to 15 regular files
        int regularFilesRead = 0;
        for (ZipFileEntry fileEntry : allFiles) {
            if (regularFilesRead >= 15) {
                break;
            }

            try {
                String content = new String(fileEntry.content, StandardCharsets.UTF_8);
                
                if (content.length() > 3000) {
                    content = content.substring(0, 3000) + "\n... (truncated)";
                }

                filesContent.put(fileEntry.name, content);
                regularFilesRead++;
                System.out.println("✓ Read file: " + fileEntry.name);
            } catch (Exception e) {
                System.err.println("✗ Failed to read file: " + fileEntry.name);
            }
        }

        System.out.println("Total files read for analysis: " + filesContent.size());
        return filesContent;
    }

    // Helper class to store ZIP entry data
    private static class ZipFileEntry {
        String name;
        byte[] content;

        ZipFileEntry(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }
    }

    // Helper method to list files in ZIP
    private List<FileInfo> listFilesInZip(byte[] zipBytes, String prefix) throws IOException {
        List<FileInfo> fileInfos = new ArrayList<>();
        Set<String> addedDirectories = new HashSet<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Filter by prefix if provided
                if (prefix != null && !prefix.isEmpty()) {
                    if (!entryName.startsWith(prefix + "/") && !entryName.equals(prefix)) {
                        continue;
                    }
                    // Remove prefix from entry name
                    entryName = entryName.substring(prefix.length());
                    if (entryName.startsWith("/")) {
                        entryName = entryName.substring(1);
                    }
                }

                // Skip if empty
                if (entryName.isEmpty()) {
                    continue;
                }

                // Get first level only
                String[] parts = entryName.split("/");
                String firstName = parts[0];

                if (parts.length > 1) {
                    // It's in a subdirectory, add the directory
                    if (!addedDirectories.contains(firstName)) {
                        String dirPath = prefix != null && !prefix.isEmpty() ? 
                                prefix + "/" + firstName : firstName;
                        fileInfos.add(new FileInfo(firstName, dirPath, true, "-"));
                        addedDirectories.add(firstName);
                    }
                } else {
                    // It's a file in current directory
                    String filePath = prefix != null && !prefix.isEmpty() ? 
                            prefix + "/" + firstName : firstName;
                    String size = formatFileSize(entry.getSize());
                    fileInfos.add(new FileInfo(firstName, filePath, false, size));
                }

                zis.closeEntry();
            }
        }

        return fileInfos;
    }

    // Helper method to extract file content from ZIP
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

    // Helper method to extract file bytes from ZIP
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
