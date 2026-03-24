package com.workproofpay.backend.shared.bootstrap;

import com.workproofpay.backend.advance.repo.AdvancePolicyRepository;
import com.workproofpay.backend.advance.service.AdvancePolicyDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DevAdvancePolicyInitializer implements CommandLineRunner {

    private final AdvancePolicyRepository advancePolicyRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (advancePolicyRepository.findFirstByOrderByIdAsc().isPresent()) {
            return;
        }
        advancePolicyRepository.save(AdvancePolicyDefaults.createDefault());
    }
}
