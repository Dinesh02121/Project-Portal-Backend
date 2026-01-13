package projectPortal.com.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
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

    @Autowired
    private SupabaseStorageService supabaseStorage;

    public FacultyDashboardService(
            FacultyRepository facultyRepository, 
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository) {
        this.facultyRepository = facultyRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
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
        details.setProjectPath(project.getExtractedPath());
        details.setStatus(project.getStatus().toString());
        details.setCollege(project.getCollege());
        details.setProgress(project.getProgress());
        details.setSubmittedAt(project.getSubmittedAt() != null ?
                project.getSubmittedAt().toString() : null);

        // Add student details
        StudentEntity student = project.getCreatedBy();
        if (student != null) {
            details.setStudentName(student.getStudentName());
            details.setStudentEmail(student.getUser().getEmail());
            details.setStudentRollNo(student.getRollNo());
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

    // ==================== FILE OPERATIONS WITH SUPABASE ====================

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

        try {
            // Download ZIP from Supabase
            String zipPath = project.getProjectZipPath();
            if (zipPath == null || zipPath.isEmpty()) {
                throw new RuntimeException("Project ZIP not found in storage");
            }

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
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("File path is required");
        }

        FacultyEntity faculty = facultyRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify faculty is assigned to this project
        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        try {
            // Download ZIP from Supabase
            String zipPath = project.getProjectZipPath();
            if (zipPath == null || zipPath.isEmpty()) {
                throw new RuntimeException("Project ZIP not found in storage");
            }

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
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("File path is required");
        }

        FacultyEntity faculty = facultyRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Verify faculty is assigned to this project
        if (!project.getAssignedFaculty().getFacultyId().equals(faculty.getFacultyId())) {
            throw new RuntimeException("Unauthorized: You are not assigned to this project");
        }

        try {
            // Download ZIP from Supabase
            String zipPath = project.getProjectZipPath();
            if (zipPath == null || zipPath.isEmpty()) {
                throw new RuntimeException("Project ZIP not found in storage");
            }

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

    // ==================== HELPER METHODS ====================

    private List<FileInfo> listFilesInZip(byte[] zipBytes, String prefix) throws IOException {
        List<FileInfo> fileInfos = new ArrayList<>();
        Set<String> addedDirectories = new HashSet<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Skip macOS metadata files
                if (entryName.contains("__MACOSX") || entryName.contains(".DS_Store")) {
                    continue;
                }

                // Filter by prefix if provided
                if (prefix != null && !prefix.isEmpty()) {
                    String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
                    
                    if (!entryName.startsWith(normalizedPrefix) && !entryName.equals(prefix)) {
                        continue;
                    }
                    
                    // Remove prefix from entry name
                    entryName = entryName.substring(normalizedPrefix.length());
                }

                // Skip if empty or just "/"
                if (entryName.isEmpty() || entryName.equals("/")) {
                    continue;
                }

                // Remove trailing slash for processing
                boolean isDirectory = entryName.endsWith("/") || entry.isDirectory();
                if (entryName.endsWith("/")) {
                    entryName = entryName.substring(0, entryName.length() - 1);
                }

                // Skip if still empty
                if (entryName.isEmpty()) {
                    continue;
                }

                // Get first level only
                String[] parts = entryName.split("/");
                String firstName = parts[0];

                if (parts.length > 1) {
                    // It's in a subdirectory, add the directory if not already added
                    if (!addedDirectories.contains(firstName)) {
                        String dirPath = prefix != null && !prefix.isEmpty() ? 
                                prefix + "/" + firstName : firstName;
                        fileInfos.add(new FileInfo(firstName, dirPath, true, "-"));
                        addedDirectories.add(firstName);
                    }
                } else {
                    // It's in current directory
                    if (isDirectory) {
                        // Add directory if not already added
                        if (!addedDirectories.contains(firstName)) {
                            String dirPath = prefix != null && !prefix.isEmpty() ? 
                                    prefix + "/" + firstName : firstName;
                            fileInfos.add(new FileInfo(firstName, dirPath, true, "-"));
                            addedDirectories.add(firstName);
                        }
                    } else {
                        // It's a file
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

        // Sort: directories first, then files, both alphabetically
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
