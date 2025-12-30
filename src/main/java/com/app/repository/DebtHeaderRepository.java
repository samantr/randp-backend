package com.app.repository;

import com.app.model.DebtHeader;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtHeaderRepository extends JpaRepository<DebtHeader, Long> {
}
