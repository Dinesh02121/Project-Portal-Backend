package projectPortal.com.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import projectPortal.com.DTO.CollegeAdmin;
import projectPortal.com.Entity.CollegeAdminEntity;
import projectPortal.com.Entity.UserEntity;
import projectPortal.com.Repository.CollegeAdminRepo;
import projectPortal.com.Repository.UserRepository;
import projectPortal.com.enums.Role;


@Service
public class CollegeAdminRegistration {
   private final UserRepository userRepository;
   private final PasswordEncoder passwordEncoder;
   private final CollegeAdminRepo collegeAdminRepo;

    public CollegeAdminRegistration(UserRepository userRepository, PasswordEncoder passwordEncoder, CollegeAdminRepo collegeAdminRepo) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.collegeAdminRepo = collegeAdminRepo;
    }


    public String collegeAdminRegistration(CollegeAdmin collegeAdmin){
        if(userRepository.findByEmail(collegeAdmin.getEmail()).isPresent()){
            throw new RuntimeException("Email id Already Exist");
        }

        UserEntity user=new UserEntity();
        user.setEmail(collegeAdmin.getEmail());
        user.setPassword(passwordEncoder.encode(collegeAdmin.getPassword()));
        user.setEnabled(true);
        user.setRole(Role.COLLEGE);
        userRepository.save(user);
        CollegeAdminEntity admin = new CollegeAdminEntity();
        admin.setUser(user);
        collegeAdminRepo.save(admin);

        return "College Registered Successfully";


    }
}
