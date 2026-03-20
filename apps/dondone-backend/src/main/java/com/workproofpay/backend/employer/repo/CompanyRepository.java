package com.workproofpay.backend.employer.repo;

import com.workproofpay.backend.employer.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
