package com.awbd.auth.repositories;

import com.awbd.auth.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRole(User.Role role);


    // nr useri cu un anumit rol
    Long countByRole(User.Role role);
}