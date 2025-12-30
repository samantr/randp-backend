package com.app.repository;

import com.app.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndIdNot(String title, Long id);

    long countByParent_Id(Long parentId);

    List<Project> findByParent_Id(Long parentId);
}
