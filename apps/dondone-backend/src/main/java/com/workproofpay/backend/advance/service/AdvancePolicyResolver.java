package com.workproofpay.backend.advance.service;

import com.workproofpay.backend.advance.model.AdvancePolicy;
import com.workproofpay.backend.advance.repo.AdvancePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdvancePolicyResolver {

    private final AdvancePolicyRepository advancePolicyRepository;

    public AdvancePolicy resolve() {
        return advancePolicyRepository.findFirstByOrderByIdAsc()
                .orElseGet(AdvancePolicyDefaults::createDefault);
    }
}
