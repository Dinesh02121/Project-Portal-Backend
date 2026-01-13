package projectPortal.com.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import projectPortal.com.DTO.FacultyDashboardSummary;
import projectPortal.com.DTO.FacultyProfile;
import projectPortal.com.DTO.FileInfo;
import projectPortal.com.DTO.ProjectDetailsDTO;
import projectPortal.com.Entity.ProjectEntity;
import projectPortal.com.Service.FacultyDashboardService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/faculty/dashboard")
public class FacultyDashboardController {
    
    @Autowired
    private FacultyDashboardService dashboardService;

    @GetMapping("/summary")
    public FacultyDashboardSummary summary(Authentication auth) {
        return dashboardService.getSummary(auth.getName());
    }

    @GetMapping("/profile")
    public FacultyProfile facultyProfile(Authentication auth){
        return dashboardService.facultyProfile(auth.getName());
    }

    @GetMapping("/requests")
    public List<ProjectEntity> requests(Authentication auth) {
        return dashboardService.getRequestedProjects(auth.getName());
    }

    @GetMapping("/projects/all")
    public List<ProjectEntity> allProjects(Authentication auth) {
        return dashboardService.getAllAssignedProjects(auth.getName());
    }

    @GetMapping("/projects/accepted")
    public List<ProjectEntity> acceptedProjects(Authentication auth) {
        return dashboardService.getAcceptedProjects(auth.getName());
    }

    @PutMapping("/project/{projectId}/decision")
    public String decision(
            @PathVariable Long projectId,
            @RequestParam boolean accept,
            Authentication auth) {
        return dashboardService.respondToProject(
                projectId,
                accept,
                auth.getName()
        );
    }

    @PostMapping("/project/{projectId}/progress")
    public String setProgress(
            @PathVariable Long projectId,
            @RequestBody Map<String, Integer> request,
            Authentication auth) {
        return dashboardService.setProgress(projectId, request.get("progress"));
    }

    @GetMapping("/project/{projectId}/details")
    public ResponseEntity<?> getProjectDetails(
            @PathVariable Long projectId,
            Authentication auth) {
        return ResponseEntity.ok(dashboardService.getProjectDetails(projectId, auth.getName()));
    }

    // File operations endpoints
    @GetMapping("/project/{projectId}/files")
    public List<FileInfo> getProjectFiles(
            @PathVariable Long projectId,
            @RequestParam(required = false) String path,
            Authentication auth) {
        return dashboardService.getProjectFiles(projectId, path, auth.getName());
    }

    @GetMapping("/project/{projectId}/file/content")
    public ResponseEntity<String> getFileContent(
            @PathVariable Long projectId,
            @RequestParam String path,
            Authentication auth) {
        String content = dashboardService.getFileContent(projectId, path, auth.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

            @PostMapping("/project/{projectId}/ai-analysis")
            public ResponseEntity<String> runAIAnalysis(
                    @PathVariable Long projectId,
                    Authentication auth) {
                try {
                    String result = dashboardService.runAIAnalysis(projectId, auth.getName());
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(result);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("{\"error\": \"" + e.getMessage() + "\"}");
                }
            }

    @GetMapping("/project/{projectId}/file/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long projectId,
            @RequestParam String path,
            Authentication auth) {
        Resource resource = dashboardService.downloadFile(projectId, path, auth.getName());
        String filename = path.substring(path.lastIndexOf('/') + 1);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }
}

