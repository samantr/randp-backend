package com.app.repository;

import com.app.model.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PersonRepository extends JpaRepository<Person, Long> {

    // Search by name / lastName / companyName / tel
    @Query("""
        select p from Person p
        where
            (:q is null or :q = '' or
             lower(coalesce(p.name, '')) like lower(concat('%', :q, '%')) or
             lower(coalesce(p.lastName, '')) like lower(concat('%', :q, '%')) or
             lower(coalesce(p.companyName, '')) like lower(concat('%', :q, '%')) or
             lower(coalesce(p.tel, '')) like lower(concat('%', :q, '%')))
        """)
    Page<Person> search(@Param("q") String q, Pageable pageable);

    // Uniqueness checks (application-level; DB doesn’t have uniques here)
    boolean existsByIsLegalTrueAndCompanyNameIgnoreCase(String companyName);

    @Query("""
        select (count(p) > 0) from Person p
        where p.isLegal = false
          and lower(coalesce(p.name,'')) = lower(coalesce(:name,''))
          and lower(coalesce(p.lastName,'')) = lower(coalesce(:lastName,''))
        """)
    boolean existsNatural(@Param("name") String name, @Param("lastName") String lastName);

    boolean existsByIsLegalTrueAndCompanyNameIgnoreCaseAndIdNot(String companyName, Long id);

    @Query("""
        select (count(p) > 0) from Person p
        where p.isLegal = false
          and lower(coalesce(p.name,'')) = lower(coalesce(:name,''))
          and lower(coalesce(p.lastName,'')) = lower(coalesce(:lastName,''))
          and p.id <> :id
        """)
    boolean existsNaturalExcludingId(@Param("name") String name, @Param("lastName") String lastName, @Param("id") Long id);
}
