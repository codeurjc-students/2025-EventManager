package eventManager.repository;

import java.util.Optional;

import eventManager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User,Integer> , JpaSpecificationExecutor<User> {
    
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Boolean  existsByEmailOrUsername(String email, String username);
    
}