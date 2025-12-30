package com.app.repository;

import com.app.model.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {

    boolean existsByTitleIgnoreCase(String title);

    Optional<ItemCategory> findByTitleIgnoreCase(String title);

    List<ItemCategory> findByParentIsNullOrderByTitleAsc();

    List<ItemCategory> findAllByOrderByTitleAsc();

    boolean existsByParent_Id(Long parentId);
}
