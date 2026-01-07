package projectPortal.com.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectPortal.com.Entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity,Long>{
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailAndRole(String email, String role);
    List<UserEntity> findByRole(String role);
}
