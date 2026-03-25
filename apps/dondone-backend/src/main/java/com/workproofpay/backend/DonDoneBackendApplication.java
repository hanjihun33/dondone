package com.workproofpay.backend;

import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.vault.config.VaultProperties;
import com.workproofpay.backend.workproof.config.DefaultWorkContractProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({RemittanceProperties.class, VaultProperties.class, DefaultWorkContractProperties.class})
public class DonDoneBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DonDoneBackendApplication.class, args);
    }
}
