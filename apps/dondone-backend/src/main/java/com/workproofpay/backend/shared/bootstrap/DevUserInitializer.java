package com.workproofpay.backend.shared.bootstrap;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DevUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("test@gmail.com")) {
            return;
        }
        User user = User.register(
                "test@gmail.com",
                passwordEncoder.encode("qweqwe123"),
                "Test User",
                "01012345678"
        );
        userRepository.save(user);
    }
}
