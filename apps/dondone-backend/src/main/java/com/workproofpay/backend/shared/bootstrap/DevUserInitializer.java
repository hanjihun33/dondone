package com.workproofpay.backend.shared.bootstrap;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.auth.support.EmailNormalizer;
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
        String normalizedEmail = EmailNormalizer.normalize("test@gmail.com");

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }
        User user = User.register(
                normalizedEmail,
                passwordEncoder.encode("qweqwe123"),
                "Test User",
                "01012345678"
        );
        userRepository.save(user);
    }
}
