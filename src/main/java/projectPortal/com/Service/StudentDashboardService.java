package projectPortal.com.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import projectPortal.com.DTO.FileInfo;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;


@Service
public class StudentDashboardService {
    @Autowired
    private EmailService emailService;

    @Value("${file.upload.directory}")
    private String uploadDir;
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
            CollegeRepository collegeRepository, UserRepository userRepository) {
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
            Files.createDirectories(Paths.get(uploadDir));

            String zipName = UUID.randomUUID() + "_" + zipFile.getOriginalFilename();
            Path zipPath = Paths.get(uploadDir, zipName);

            Files.copy(zipFile.getInputStream(), zipPath,
                    StandardCopyOption.REPLACE_EXISTING);

            ProjectEntity project = new ProjectEntity();
            project.setTitle(request.getTitle());
            project.setDescription(request.getDescription());
            project.setCreatedBy(student);
            project.setCollege(college.getCollegeName());
            project.setAssignedFaculty(faculty);
            project.setProjectZipName(zipName);
            project.setProjectZipPath(zipPath.toString());
            project.setStatus(ProjectStatus.FACULTY_REQUESTED);
            project.setProgress(0);
            project.setSubmittedAt(LocalDateTime.now());

            projectRepository.save(project);

            String extractPath = uploadDir + "/extracted/" + project.getProjectId();
            ZipUtil.unzip(project.getProjectZipPath(), extractPath);
            project.setExtractedPath(extractPath);
            projectRepository.save(project);

            ProjectMemberEntity leader = new ProjectMemberEntity();
            leader.setProject(project);
            leader.setStudent(student);
            leader.setRole(MemberRole.LEADER);
            projectMemberRepository.save(leader);

            String body="Hello "+ faculty.getFacultyName() +",\n" +
                    "\n" +
                    "You have received a new project supervision request from a student.\n" +
                    "\n" +
                    "Project Details:\n" +
                    "• Project Title: "+ " "+ project.getTitle() + " \n " +
                    "• Student Name: " +  student.getStudentName()  + " \n" +
                    "• College: " + student.getCollege() + "\n" +
                    "\n" +
                    "Please log in to the Project Portal to review the project details and take action.\n" +
                    "\n" +
                    "Actions Available:\n" +
                    "• Accept the project\n" +
                    "• Reject the project \n" +
                    "\n" +
                    "Kindly review and respond at your earliest convenience.\n" +
                    "\n" +
                    "Regards,\n" +
                    "Project Portal Team\n";
            String subject="Project Review Mail";
            emailService.sendEmail(subject,faculty.getUser().getEmail(),body);

            return "Project created and faculty request sent successfully";

        } catch (IOException e) {
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

        String extractedPath = project.getExtractedPath();
        if (extractedPath == null || extractedPath.isEmpty()) {
            throw new RuntimeException("Project files not extracted");
        }

        System.out.println("Extracted Path: " + extractedPath);

        Path basePath = Paths.get(extractedPath);

        // If relativePath is provided, resolve it, otherwise use base path
        Path targetPath = (relativePath != null && !relativePath.isEmpty())
                ? basePath.resolve(relativePath).normalize()
                : basePath;

        System.out.println("Target Path: " + targetPath);
        System.out.println("Target exists: " + Files.exists(targetPath));
        System.out.println("Target is directory: " + Files.isDirectory(targetPath));

        if (!Files.exists(targetPath)) {
            throw new RuntimeException("Path does not exist: " + targetPath);
        }

        if (!Files.isDirectory(targetPath)) {
            throw new RuntimeException("Path is not a directory: " + targetPath);
        }

        List<FileInfo> fileInfos = new ArrayList<>();

        try (Stream<Path> paths = Files.list(targetPath)) {
            paths.forEach(path -> {
                try {
                    File file = path.toFile();
                    String fileName = file.getName();

                    // Build the relative path from the extracted root
                    String filePath;
                    if (relativePath != null && !relativePath.isEmpty()) {
                        filePath = relativePath + "/" + fileName;
                    } else {
                        filePath = fileName;
                    }

                    String size = file.isDirectory() ? "-" : formatFileSize(file.length());

                    System.out.println("Adding file: " + fileName + " | Path: " + filePath + " | IsDir: " + file.isDirectory());

                    fileInfos.add(new FileInfo(fileName, filePath, file.isDirectory(), size));
                } catch (Exception e) {
                    System.err.println("Error processing file: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error reading directory: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error reading project files: " + e.getMessage());
        }

        System.out.println("Total files found: " + fileInfos.size());
        return fileInfos;
    }

    public String getFileContent(Long projectId, String relativePath, String email) {
        System.out.println("=== GET FILE CONTENT DEBUG ===");
        System.out.println("Project ID: " + projectId);
        System.out.println("Relative Path: " + relativePath);
        System.out.println("Email: " + email);

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

        String extractedPath = project.getExtractedPath();
        if (extractedPath == null || extractedPath.isEmpty()) {
            throw new RuntimeException("Project files not extracted");
        }

        Path basePath = Paths.get(extractedPath);
        Path filePath = basePath.resolve(relativePath).normalize();

        System.out.println("Extracted Path: " + extractedPath);
        System.out.println("Full File Path: " + filePath);
        System.out.println("File exists: " + Files.exists(filePath));
        System.out.println("File is readable: " + Files.isReadable(filePath));
        System.out.println("File is directory: " + Files.isDirectory(filePath));

        // Security check: ensure the resolved path is still within the project directory
        if (!filePath.startsWith(basePath)) {
            throw new RuntimeException("Invalid file path - path traversal attempt detected");
        }

        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + relativePath);
        }

        if (Files.isDirectory(filePath)) {
            throw new RuntimeException("Cannot read content of a directory");
        }

        try {
            String content = Files.readString(filePath);
            System.out.println("File content length: " + content.length());
            return content;
        } catch (Exception e) {
            System.err.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error reading file: " + e.getMessage());
        }
    }

    public Resource downloadFile(Long projectId, String relativePath, String email) {
        System.out.println("=== DOWNLOAD FILE DEBUG ===");
        System.out.println("Project ID: " + projectId);
        System.out.println("Relative Path: " + relativePath);
        System.out.println("Email: " + email);

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

        String extractedPath = project.getExtractedPath();
        if (extractedPath == null || extractedPath.isEmpty()) {
            throw new RuntimeException("Project files not extracted");
        }

        Path basePath = Paths.get(extractedPath);
        Path filePath = basePath.resolve(relativePath).normalize();

        System.out.println("Extracted Path: " + extractedPath);
        System.out.println("Full File Path: " + filePath);
        System.out.println("File exists: " + Files.exists(filePath));
        System.out.println("File is readable: " + Files.isReadable(filePath));
        System.out.println("File is directory: " + Files.isDirectory(filePath));


        if (!filePath.startsWith(basePath)) {
            throw new RuntimeException("Invalid file path - path traversal attempt detected");
        }

        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + relativePath);
        }


        if (Files.isDirectory(filePath)) {
            System.err.println("BLOCKED: Attempt to download directory: " + filePath);
            throw new RuntimeException("Cannot download folders. Only individual files can be downloaded. Please select a file instead.");
        }

        if (!Files.isReadable(filePath)) {
            throw new RuntimeException("File is not readable");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                System.out.println("Resource created successfully for file: " + filePath.getFileName());
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + filePath);
            }
        } catch (Exception e) {
            System.err.println("Error creating resource: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error downloading file: " + e.getMessage());
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    @Transactional
    public String deleteProject(Long projectId, String email){
        System.out.println("Attempting to delete project: " + projectId);

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("No Project Exist With this Project Id"));


        StudentEntity student = studentRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (!project.getCreatedBy().getStudentId().equals(student.getStudentId())) {
            throw new RuntimeException("You don't have permission to delete this project");
        }

        try {

            projectMemberRepository.deleteByProject(project);

            deleteProjectFiles(project);


            projectRepository.delete(project);

            return "Project Deleted Successfully";
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete project: " + e.getMessage());
        }
    }

    private void deleteProjectFiles(ProjectEntity project) {
        try {

            if (project.getExtractedPath() != null) {
                Path extractedPath = Paths.get(project.getExtractedPath());
                if (Files.exists(extractedPath)) {
                    deleteDirectory(extractedPath);
                }
            }


            if (project.getProjectZipPath() != null) {
                Path zipPath = Paths.get(project.getProjectZipPath());
                if (Files.exists(zipPath)) {
                    Files.delete(zipPath);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not delete project files: " + e.getMessage());

        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> files = Files.walk(path)) {
                files.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                System.err.println("Could not delete: " + p);
                            }
                        });
            }
        }
    }


    public String deleteStudentProfile(String email){
        StudentEntity student=studentRepository.findByUser_Email(email).orElseThrow(
                ()-> new RuntimeException("No Student Found With This Email")
        );

        studentRepository.delete(student);

        UserEntity user=userRepository.findByEmail(email).orElseThrow(
                ()-> new RuntimeException("Unable to find Student")
        );

        userRepository.delete(user);


        return "Student Profile Deleted Successfully";
    }

}