package com.cleantrack.laundry_system.repository;

import com.cleantrack.laundry_system.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    java.util.List<User> findByRoleNot(com.cleantrack.laundry_system.model.Role role);
}
