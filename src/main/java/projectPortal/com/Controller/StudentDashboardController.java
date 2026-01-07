package projectPortal.com.Controller;
import projectPortal.com.DTO.FileInfo;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import projectPortal.com.DTO.CreateProjectRequest;
import projectPortal.com.DTO.StudentDashboardSummary;
import projectPortal.com.DTO.StudentProfileResponse;
import projectPortal.com.DTO.StudentProjectResponse;
import projectPortal.com.Entity.ProjectEntity;
import projectPortal.com.Service.StudentDashboardService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;


@RestController
@RequestMapping("/auth/student/dashboard")
public class StudentDashboardController {

    private final StudentDashboardService dashboardService;

    public StudentDashboardController(StudentDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/profile")
    public StudentProfileResponse getProfile(Authentication auth) {
        return dashboardService.getProfile(auth.getName());
    }

    @GetMapping("/summary")
    public StudentDashboardSummary summary(Authentication auth) {
        return dashboardService.getSummary(auth.getName());
    }

    @GetMapping("/recent-projects")
    public List<ProjectEntity> recentProjects(Authentication auth) {
        return dashboardService.recentProjects(auth.getName());
    }

    @PostMapping(value = "/create/project", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createProject(
            @RequestPart("data") CreateProjectRequest request,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        try {
            String result = dashboardService.createProject(
                    request,
                    file,
                    authentication.getName()
            );
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/projects")
    public List<StudentProjectResponse> myProjects(Authentication authentication) {
        return dashboardService.getAllMyProjects(authentication.getName());
    }

    @GetMapping("/project/{projectId}/files")
    public ResponseEntity<?> getProjectFiles(
            @PathVariable Long projectId,
            @RequestParam(required = false) String path,
            Authentication authentication) {

        System.out.println("=== CONTROLLER: Get Project Files ===");
        System.out.println("Project ID: " + projectId);
        System.out.println("Path param: " + path);
        System.out.println("User: " + authentication.getName());

        try {
            List<FileInfo> files = dashboardService.getProjectFiles(
                    projectId,
                    path,
                    authentication.getName()
            );
            return ResponseEntity.ok(files);
        } catch (RuntimeException e) {
            System.err.println("Error in getProjectFiles: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/project/{projectId}/file/content")
    public ResponseEntity<?> getFileContent(
            @PathVariable Long projectId,
            @RequestParam String path,
            Authentication authentication) {

        System.out.println("=== CONTROLLER: Get File Content ===");
        System.out.println("Project ID: " + projectId);
        System.out.println("Path param: " + path);
        System.out.println("User: " + authentication.getName());

        try {
            String content = dashboardService.getFileContent(
                    projectId,
                    path,
                    authentication.getName()
            );
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(content);
        } catch (RuntimeException e) {
            System.err.println("Error in getFileContent: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/project/{projectId}/file/download")
    public ResponseEntity<?> downloadFile(
            @PathVariable Long projectId,
            @RequestParam String path,
            Authentication authentication) {

        System.out.println("=== CONTROLLER: Download File ===");
        System.out.println("Project ID: " + projectId);
        System.out.println("Path param: " + path);
        System.out.println("User: " + authentication.getName());

        try {
            Resource file = dashboardService.downloadFile(
                    projectId,
                    path,
                    authentication.getName()
            );


            String filename = file.getFilename();
            if (filename == null) {
                filename = "download";
            }


            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            System.out.println("Sending file: " + filename);
            System.out.println("File size: " + file.contentLength() + " bytes");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(file);

        } catch (RuntimeException e) {
            System.err.println("Error in downloadFile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error in downloadFile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("delete-student")
    public String deleteProfile(Authentication authentication){
        return dashboardService.deleteStudentProfile(authentication.getName());

    }

    @DeleteMapping("delete-project/{projectId}")
    public String deleteProject(@PathVariable Long projectId,Authentication authentication){
        return dashboardService.deleteProject(projectId, authentication.getName());
    }
}