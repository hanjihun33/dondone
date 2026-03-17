package com.workproofpay.remittance;

import com.workproofpay.remittance.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class RemittanceApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RemittanceApiApplication.class, args);
    }
}
