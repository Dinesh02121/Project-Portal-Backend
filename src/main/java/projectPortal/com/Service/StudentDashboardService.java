package projectPortal.com.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import projectPortal.com.DTO.FileInfo;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import projectPortal.com.DTO.CreateProjectRequest;
import projectPortal.com.DTO.StudentDashboardSummary;
import projectPortal.com.DTO.StudentProfileResponse;
import projectPortal.com.DTO.StudentProjectResponse;
import projectPortal.com.Entity.*;
import projectPortal.com.Repository.*;
import projectPortal.com.enums.MemberRole;
import projectPortal.com.enums.ProjectStatus;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class StudentDashboardService {
    
    @Autowired
    private EmailService emailService;

    @Autowired
    private SupabaseStorageService supabaseStorage;

    private final StudentRepository studentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final FacultyRepository facultyRepository;
    private final CollegeRepository collegeRepository;
    private final UserRepository userRepository;

    public StudentDashboardService(
            StudentRepository studentRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            FacultyRepository facultyRepository,
            CollegeRepository collegeRepository,
            UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.facultyRepository = facultyRepository;
        this.collegeRepository = collegeRepository;
        this.userRepository = userRepository;
    }

    public StudentProfileResponse getProfile(String email) {
        StudentEntity student = studentRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        return new StudentProfileResponse(
                student.getStudentId(),
                student.getStudentName(),
                student.getRollNo(),
                student.getBranch(),
                student.getSemester(),
                student.getUser().getEmail(),
                student.getCollege() != null ?
                        student.getCollege().getCollegeName() : "Unknown College"
        );
    }

    public StudentDashboardSummary getSummary(String email) {
        StudentEntity student = studentRepository
                .findByUser_Email(email)
                .orElseThrow();

        long active = projectRepository.countActive(student);
        long review = projectRepository.countUnderReview(student);
        long completed = projectRepository.countCompleted(student);
        long collaboration = projectMemberRepository.countByStudent(student);
        long draft = 0;

        return new StudentDashboardSummary(active, review, completed, collaboration, draft);
    }

    public List<ProjectEntity> recentProjects(String email) {
        StudentEntity student = studentRepository
                .findByUser_Email(email)
                .orElseThrow();

        return projectRepository
                .findTop5ByCreatedByOrderBySubmittedAtDesc(student);
    }

    public String createProject(CreateProjectRequest request,
                                MultipartFile zipFile,
                                String email) {

        StudentEntity student = studentRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        CollegeEntity college = collegeRepository.findById(request.getCollegeId())
                .orElseThrow(() -> new RuntimeException("College not found"));

        FacultyEntity faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        if (!faculty.getCollege().getCollegeId().equals(college.getCollegeId())) {
            throw new RuntimeException("Faculty does not belong to selected college");
        }

        if (zipFile == null || zipFile.isEmpty()) {
            throw new RuntimeException("Project ZIP is required");
        }

        if (!zipFile.getOriginalFilename().endsWith(".zip")) {
            throw new RuntimeException("Only ZIP files are allowed");
        }

        try {
            // Create project entity first to get project ID
            ProjectEntity project = new ProjectEntity();
            project.setTitle(request.getTitle());
            project.setDescription(request.getDescription());
            project.setCreatedBy(student);
            project.setCollege(college.getCollegeName());
            project.setAssignedFaculty(faculty);
            project.setStatus(ProjectStatus.FACULTY_REQUESTED);
            project.setProgress(0);
            project.setSubmittedAt(LocalDateTime.now());

            // Save to get project ID
            projectRepository.save(project);

            Long projectId = project.getProjectId();
            
            // Create Supabase path: Project-Submission/projects/{id}/{filename}
            String supabasePath = "Project-Submission/projects/" + projectId + "/" + 
                                  zipFile.getOriginalFilename();

            // Upload ZIP to Supabase
            byte[] zipBytes = zipFile.getBytes();
            String uploadedPath = supabaseStorage.uploadFile(supabasePath, zipBytes);

            // Update project with Supabase paths
            project.setProjectZipName(zipFile.getOriginalFilename());
            project.setProjectZipPath(uploadedPath); // Full path in Supabase
            project.setExtractedPath("projects/" + projectId + "/extracted"); // Virtual path

            projectRepository.save(project);

            // Create project leader
            ProjectMemberEntity leader = new ProjectMemberEntity();
            leader.setProject(project);
            leader.setStudent(student);
            leader.setRole(MemberRole.LEADER);
            projectMemberRepository.save(leader);

            // Send email to faculty
            String body = "Hello " + faculty.getFacultyName() + ",\n\n" +
                    "You have received a new project supervision request from a student.\n\n" +
                    "Project Details:\n" +
                    "• Project Title: " + project.getTitle() + "\n" +
                    "• Student Name: " + student.getStudentName() + "\n" +
                    "• College: " + student.getCollege() + "\n\n" +
                    "Please log in to the Project Portal to review the project details and take action.\n\n" +
                    "Actions Available:\n" +
                    "• Accept the project\n" +
                    "• Reject the project\n\n" +
                    "Kindly review and respond at your earliest convenience.\n\n" +
                    "Regards,\nProject Portal Team\n";
            String subject = "Project Review Mail";
            emailService.sendEmail(subject, faculty.getUser().getEmail(), body);

            return "Project created and faculty request sent successfully";

        } catch (IOException e) {
            System.err.println("Error creating project: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Project creation failed: " + e.getMessage());
        }
    }

    public List<StudentProjectResponse> getAllMyProjects(String email) {
        StudentEntity student = studentRepository
                .findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<ProjectEntity> projects = projectRepository.findByCreatedBy(student);

        return projects.stream().map(project -> {
            String facultyName = project.getAssignedFaculty() != null
                    ? project.getAssignedFaculty().getFacultyName()
                    : "Not Assigned";

            boolean isTeamProject = projectMemberRepository.countByProject(project) > 1;

            return new StudentProjectResponse(
                    project.getProjectId(),
                    project.getTitle(),
                    project.getStatus().name(),
                    project.getProgress(),
                    facultyName,
                    isTeamProject,
                    project.getSubmittedAt()
            );
        }).toList();
    }

    public List<FileInfo> getProjectFiles(Long projectId, String relativePath, String email) {
        StudentEntity student = studentRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getCreatedBy().getStudentId().equals(student.getStudentId())) {
            throw new RuntimeException("Unauthorized access to project");
        }

        try {
            // Download ZIP from Supabase
            String zipPath = project.getProjectZipPath();
            byte[] zipBytes = supabaseStorage.downloadFile(zipPath);

            // List files in ZIP
            return listFilesInZip(zipBytes, relativePath);

        } catch (Exception e) {
            System.err.println("Error reading project files: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error reading project files: " + e.getMessage());
        }
    }

    public String getFileContent(Long projectId, String relativePath, String email) {
        if (relativePath == null || relativePath.isEmpty()) {
            throw new RuntimeException("File path is required");
        }

        StudentEntity student = studentRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getCreatedBy().getStudentId().equals(student.getStudentId())) {
            throw new RuntimeException("Unauthorized access to project");
        }

        try {
            // Download ZIP from Supabase
            String zipPath = project.getProjectZipPath();
            byte[] zipBytes = supabaseStorage.downloadFile(zipPath);

            // Extract specific file content from ZIP
            return extractFileFromZip(zipBytes, relativePath);

        } catch (Exception e) {
            System.err.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error reading file: " + e.getMessage());
        }
    }

    public Resource downloadFile(Long projectId, String relativePath, String email) {
        if (relativePath == null || relativePath.isEmpty()) {
            throw new RuntimeException("File path is required");
        }

        StudentEntity student = studentRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getCreatedBy().getStudentId().equals(student.getStudentId())) {
            throw new RuntimeException("Unauthorized access to project");
        }

        try {
            // Download ZIP from Supabase
            String zipPath = project.getProjectZipPath();
            byte[] zipBytes = supabaseStorage.downloadFile(zipPath);

            // Extract specific file from ZIP
            byte[] fileBytes = extractFileBytesFromZip(zipBytes, relativePath);
            
            return new ByteArrayResource(fileBytes);

        } catch (Exception e) {
            System.err.println("Error downloading file: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error downloading file: " + e.getMessage());
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

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    @Transactional
    public String deleteProject(Long projectId, String email) {
        System.out.println("Attempting to delete project: " + projectId);

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("No Project Exist With this Project Id"));

        StudentEntity student = studentRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (!project.getCreatedBy().getStudentId().equals(student.getStudentId())) {
            throw new RuntimeException("You don't have permission to delete this project");
        }

        try {
            // Delete project members
            projectMemberRepository.deleteByProject(project);

            // Delete files from Supabase
            deleteProjectFilesFromSupabase(project);

            // Delete project from database
            projectRepository.delete(project);

            return "Project Deleted Successfully";
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete project: " + e.getMessage());
        }
    }

    private void deleteProjectFilesFromSupabase(ProjectEntity project) {
        try {
            // Delete ZIP file from Supabase
            if (project.getProjectZipPath() != null) {
                supabaseStorage.deleteFile(project.getProjectZipPath());
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not delete project files from Supabase: " + e.getMessage());
        }
    }

    public String deleteStudentProfile(String email) {
        StudentEntity student = studentRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("No Student Found With This Email"));

        studentRepository.delete(student);

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Unable to find Student"));

        userRepository.delete(user);

        return "Student Profile Deleted Successfully";
    }
}
