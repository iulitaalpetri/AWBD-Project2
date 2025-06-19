package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.user.*;
import com.awbd.bookstore.models.Cart;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.models.Wishlist;
import com.awbd.bookstore.repositories.CartRepository;
import com.awbd.bookstore.repositories.UserRepository;
import com.awbd.bookstore.repositories.WishlistRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    private UserRepository userRepository;
    private CartRepository cartRepository;
    private WishlistRepository wishlistRepository;

    //pentru parola
    private static final int MIN_PASSWORD_LENGTH = 8;

    public UserService(UserRepository userRepository, CartRepository cartRepository, WishlistRepository wishlistRepository) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.wishlistRepository = wishlistRepository;
    }

    private boolean isPasswordStrong(String password) {
        return (password == null || password.length() < MIN_PASSWORD_LENGTH) ? false : true;
    }

    private boolean isUsernameValid(String username) {
        return (username == "") ? false : true;
    }

    private void validateAdminCreation() {
        long adminCount = userRepository.countByRole(User.Role.ADMIN);
        if(adminCount >= 3) {
            throw new AdminLimitExceededException("Cannot create more than 3 admin users.");
        }
    }

    @Transactional
    public User create(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new DuplicateUserException("Username already exists");
        }

        if(!isUsernameValid(user.getUsername())){
            throw new InvalidUsernameException("Username cannot be empty");
        }

        if (!isPasswordStrong(user.getPassword())) {
            throw new WeakPasswordException("Password must be at least 8 characters long");
        }

        if (user.getRole() == null || (user.getRole() != User.Role.ADMIN && user.getRole() != User.Role.USER)) {
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

            Cart cart = new Cart();
            cart.setUser(savedUser);
            cart.setBooks(new HashSet<>());
            cartRepository.save(cart);

            Wishlist wishlist = new Wishlist();
            wishlist.setUser(savedUser);
            wishlist.setBooks(new HashSet<>());
            wishlistRepository.save(wishlist);

            savedUser.setCart(cart);
            savedUser.setWishlist(wishlist);
            return savedUser;

        } catch (DataIntegrityViolationException e) {
            throw new UserCreationException("Failed to create user due to data integrity violation", e);
        }
    }

    //find user by id
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User update(Long id, User updatedUser) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    // Verifică dacă noul username există deja la alt user
                    if (!existingUser.getUsername().equals(updatedUser.getUsername()) &&
                            userRepository.existsByUsername(updatedUser.getUsername())) {
                        throw new DuplicateUserException("Username already exists");
                    }

                    existingUser.setUsername(updatedUser.getUsername());
                    if (updatedUser.getPassword() != null) {
                        // Base64 pentru encodare
                        String encodedPassword = Base64.getEncoder()
                                .encodeToString(updatedUser.getPassword().getBytes());
                        existingUser.setPassword(encodedPassword);
                    }
                    return userRepository.save(existingUser);
                })
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found."));
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User with id " + id + " not found.");
        }
        userRepository.deleteById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}