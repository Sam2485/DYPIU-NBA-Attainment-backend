package com.dypiu.nba.repository;

import com.dypiu.nba.entity.User;
import com.dypiu.nba.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);
    List<User> findByRole(UserRole role);
    List<User> findBySchoolId(String schoolId);
    List<User> findByRoleAndSchoolId(UserRole role, String schoolId);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
}
