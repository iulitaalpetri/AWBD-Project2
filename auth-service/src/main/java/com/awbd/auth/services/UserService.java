package com.awbd.auth.services;

import com.awbd.auth.clients.BookstoreClient;
import com.awbd.auth.exceptions.user.*;
import com.awbd.auth.models.User;
import com.awbd.auth.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private UserRepository userRepository;
    private BookstoreClient bookstoreClient;

    private static final int MIN_PASSWORD_LENGTH = 8;

    public UserService(UserRepository userRepository, BookstoreClient bookstoreClient) {
        this.userRepository = userRepository;
        this.bookstoreClient = bookstoreClient;
    }

    private boolean isPasswordStrong(String password) {
        return (password == null || password.length() < MIN_PASSWORD_LENGTH) ? false : true;
    }

    private boolean isUsernameValid(String username) {
        return (username == "") ? false : true;
    }

    private void validateAdminCreation() {
        logger.debug("Validating admin creation");
        long adminCount = userRepository.countByRole(User.Role.ADMIN);

        if(adminCount >= 3) {
            logger.warn("Admin creation failed - limit exceeded. Current count: {}", adminCount);
            throw new AdminLimitExceededException("Cannot create more than 3 admin users.");
        }
    }

    @Transactional
    public User create(User user) {
        logger.info("Creating user with username: {} and role: {}", user.getUsername(), user.getRole());

        if (userRepository.existsByUsername(user.getUsername())) {
            logger.warn("User creation failed - username already exists: {}", user.getUsername());
            throw new DuplicateUserException("Username already exists");
        }

        if(!isUsernameValid(user.getUsername())){
            logger.warn("User creation failed - invalid username");
            throw new InvalidUsernameException("Username cannot be empty");
        }

        if (!isPasswordStrong(user.getPassword())) {
            logger.warn("User creation failed - weak password for username: {}", user.getUsername());
            throw new WeakPasswordException("Password must be at least 8 characters long");
        }

        if (user.getRole() == null || (user.getRole() != User.Role.ADMIN && user.getRole() != User.Role.USER)) {
            logger.warn("User creation failed - invalid role: {}", user.getRole());
            throw new InvalidRoleException("Role must be either ADMIN or USER");
        }

        if (user.getRole() == User.Role.ADMIN) {
            validateAdminCreation();
        }

        String encodedPassword = Base64.getEncoder()
                .encodeToString(user.getPassword().getBytes());
        user.setPassword(encodedPassword);

        try {
            User savedUser = userRepository.save(user);
            userRepository.flush();

            logger.info("User successfully created with ID: {}", savedUser.getId());

            createUserResourcesInBookstoreService(savedUser.getId());

            return savedUser;

        } catch (DataIntegrityViolationException e) {
            logger.error("Failed to create user due to data integrity violation for username: {}",
                    user.getUsername(), e);
            throw new UserCreationException("Failed to create user due to data integrity violation", e);
        }
    }

    private void createUserResourcesInBookstoreService(Long userId) {
        logger.info("Creating cart and wishlist for user ID: {}", userId);

        try {
            bookstoreClient.createCart(userId);
            bookstoreClient.createWishlist(userId);

            logger.info("Successfully created cart and wishlist for user ID: {}", userId);

        } catch (Exception e) {
            logger.error("Failed to create cart/wishlist for user ID: {} - Error: {}",
                    userId, e.getMessage(), e);
        }
    }

    public Optional<User> findById(Long id) {
        logger.debug("Finding user by ID: {}", id);
        Optional<User> user = userRepository.findById(id);

        if (user.isEmpty()) {
            logger.debug("No user found with ID: {}", id);
        }

        return user;
    }

    public Optional<User> findByUsername(String username) {
        logger.debug("Finding user by username: {}", username);
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isEmpty()) {
            logger.debug("No user found with username: {}", username);
        }

        return user;
    }

    public User update(Long id, User updatedUser) {
        logger.info("Updating user with ID: {}", id);

        return userRepository.findById(id)
                .map(existingUser -> {
                    if (!existingUser.getUsername().equals(updatedUser.getUsername()) &&
                            userRepository.existsByUsername(updatedUser.getUsername())) {
                        logger.warn("User update failed - username already exists: {}", updatedUser.getUsername());
                        throw new DuplicateUserException("Username already exists");
                    }

                    existingUser.setUsername(updatedUser.getUsername());
                    if (updatedUser.getPassword() != null) {
                        String encodedPassword = Base64.getEncoder()
                                .encodeToString(updatedUser.getPassword().getBytes());
                        existingUser.setPassword(encodedPassword);
                    }

                    User savedUser = userRepository.save(existingUser);
                    logger.info("User successfully updated - ID: {}, username: {}", id, savedUser.getUsername());

                    return savedUser;
                })
                .orElseThrow(() -> {
                    logger.warn("User update failed - user not found with ID: {}", id);
                    return new UserNotFoundException("User with id " + id + " not found.");
                });
    }

    public void delete(Long id) {
        logger.info("Deleting user with ID: {}", id);

        if (!userRepository.existsById(id)) {
            logger.warn("User deletion failed - user not found with ID: {}", id);
            throw new UserNotFoundException("User with id " + id + " not found.");
        }

        userRepository.deleteById(id);
        logger.info("User successfully deleted with ID: {}", id);
    }

    public List<User> getAllUsers() {
        logger.debug("Retrieving all users");
        List<User> users = userRepository.findAll();
        logger.info("Retrieved {} users", users.size());
        return users;
    }
}