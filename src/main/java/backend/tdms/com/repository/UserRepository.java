package backend.tdms.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import backend.tdms.com.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByResetPasswordToken(String token);
    
    /**
     * Check if any user with admin role exists in the system.
     * This is more efficient than loading all users.
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM User u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN'")
    boolean existsUserWithAdminRole();
    
    /**
     * Count the number of users with admin role.
     * Useful for preventing deletion of the last admin.
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN'")
    long countUsersWithAdminRole();
}