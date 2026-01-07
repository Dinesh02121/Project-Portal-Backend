package projectPortal.com.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import projectPortal.com.enums.Role;

@Data
@AllArgsConstructor
public class FacultyProfile {
    private String facultyName;
    private String department;
    private String email;
    private Role role;
}
