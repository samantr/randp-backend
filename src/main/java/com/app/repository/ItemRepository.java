package com.app.repository;

import com.app.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByTitleIgnoreCase(String title);

    Optional<Item> findByCodeIgnoreCase(String code);

    Optional<Item> findByTitleIgnoreCase(String title);

    boolean existsByCategory_Id(Long categoryId);

    @Query("""
            select i from Item i
            join fetch i.category c
            where (:categoryId is null or c.id = :categoryId)
              and (
                   :q is null or :q = ''
                   or lower(i.title) like lower(concat('%', :q, '%'))
                   or lower(i.code) like lower(concat('%', :q, '%'))
              )
            order by i.title asc
            """)
    List<Item> search(@Param("q") String q, @Param("categoryId") Long categoryId);

    @Query("""
            select i from Item i
            join fetch i.category c
            order by i.title asc
            """)
    List<Item> findAllWithCategory();
}
