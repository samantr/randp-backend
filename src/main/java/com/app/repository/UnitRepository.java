package com.app.repository;

import com.app.model.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    Optional<Unit> findByTitleIgnoreCase(String title);
    boolean existsByTitleIgnoreCase(String title);
}
