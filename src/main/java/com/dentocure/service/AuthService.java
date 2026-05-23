package com.dentocure.service;

import com.dentocure.dto.LoginRequest;
import com.dentocure.dto.AuthResponse;
import com.dentocure.exception.ResourceNotFoundException;
import com.dentocure.model.User;
import com.dentocure.repository.UserRepository;
import com.dentocure.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12); // Cost factor 12

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;

    /**
     * Authenticate user and return JWT tokens
     */
    public AuthResponse login(LoginRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByUsername(loginRequest.getUsername());

        if (userOptional.isEmpty()) {
            throw new ResourceNotFoundException("Invalid username or password");
        }

        User user = userOptional.get();

        // Check if user is locked due to failed attempts
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Account is locked. Try again later.");
        }

        // Check if user is active
        if (!user.isActive()) {
            throw new RuntimeException("User account is inactive");
        }

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            // Increment failed attempts
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            // Lock account if max attempts reached
            if (user.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            }

            userRepository.save(user);
            throw new ResourceNotFoundException("Invalid username or password");
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getRole().toString());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername(), user.getRole().toString());

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getRole().toString()
        );
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String role = jwtTokenProvider.getRoleFromToken(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(username, role);

        return new AuthResponse(
                newAccessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                role
        );
    }

    /**
     * Get current authenticated user profile
     */
    public User getCurrentUserProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Change password for logged-in user
     */
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Hash password for new user creation
     */
    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    /**
     * Create a new user (admin only)
     */
    public User createUser(String username, String password, String email, String role) {
        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(com.dentocure.model.Role.valueOf(role));
        user.setActive(true);

        return userRepository.save(user);
    }
}
