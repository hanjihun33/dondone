package com.workproofpay.backend.shared.bootstrap;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.model.UserRole;
import com.workproofpay.backend.auth.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DevUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("test@gmail.com")) {
            return;
        }
        User user = new User(
                "test@gmail.com",
                passwordEncoder.encode("qweqwe123"),
                "Test User",
                UserRole.USER
        );
        userRepository.save(user);
    }
}
