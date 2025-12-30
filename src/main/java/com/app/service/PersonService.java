package com.app.service;

import com.app.dto.person.PersonCreateRequest;
import com.app.dto.person.PersonResponse;
import com.app.dto.person.PersonUpdateRequest;
import com.app.model.Person;
import com.app.repository.PersonRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final JdbcTemplate jdbcTemplate;

    public PersonService(PersonRepository personRepository, JdbcTemplate jdbcTemplate) {
        this.personRepository = personRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public PersonResponse create(PersonCreateRequest req) {
        validateBusiness(req.isLegal(), req.companyName(), req.name(), req.lastName());

        if (Boolean.TRUE.equals(req.isLegal())) {
            if (personRepository.existsByIsLegalTrueAndCompanyNameIgnoreCase(req.companyName().trim())) {
                throw new IllegalArgumentException("Company already exists (duplicate company_name).");
            }
        } else {
            if (personRepository.existsNatural(req.name(), req.lastName())) {
                throw new IllegalArgumentException("Person already exists (duplicate name + last_name).");
            }
        }

        Person p = new Person();
        apply(p, req.name(), req.lastName(), req.companyName(), req.address(), req.tel(), req.isLegal(), req.dsc());

        try {
            Person saved = personRepository.save(p);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // catches CK_persons violations too (SQL constraint)
            throw new IllegalArgumentException("Invalid person data (DB constraint violation).");
        }
    }

    @Transactional(readOnly = true)
    public Page<PersonResponse> getAll(Pageable pageable) {
        return personRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PersonResponse getById(Long id) {
        return toResponse(personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + id)));
    }

    @Transactional
    public PersonResponse update(Long id, PersonUpdateRequest req) {
        validateBusiness(req.isLegal(), req.companyName(), req.name(), req.lastName());

        Person p = personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + id));

        if (Boolean.TRUE.equals(req.isLegal())) {
            String company = req.companyName().trim();
            if (personRepository.existsByIsLegalTrueAndCompanyNameIgnoreCaseAndIdNot(company, id)) {
                throw new IllegalArgumentException("Company already exists (duplicate company_name).");
            }
        } else {
            if (personRepository.existsNaturalExcludingId(req.name(), req.lastName(), id)) {
                throw new IllegalArgumentException("Person already exists (duplicate name + last_name).");
            }
        }

        apply(p, req.name(), req.lastName(), req.companyName(), req.address(), req.tel(), req.isLegal(), req.dsc());

        try {
            Person saved = personRepository.save(p);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Invalid person data (DB constraint violation).");
        }
    }

    @Transactional
    public void delete(Long id) {
        Person p = personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + id));

        // Prevent delete if referenced by transactions or debts_header.
        // transactions.from_person_id / to_person_id are FK -> persons :contentReference[oaicite:2]{index=2}
        // debts_header.person_id is FK -> persons :contentReference[oaicite:3]{index=3}
        if (isPersonReferenced(id)) {
            throw new IllegalArgumentException("Cannot delete person: referenced by transactions/debts.");
        }

        personRepository.delete(p);
    }

    @Transactional(readOnly = true)
    public Page<PersonResponse> search(String q, Pageable pageable) {
        return personRepository.search(q, pageable).map(this::toResponse);
    }

    // ----------------- helpers -----------------

    private boolean isPersonReferenced(Long personId) {
        Integer txCount = jdbcTemplate.queryForObject(
                "select count(1) from transactions where from_person_id = ? or to_person_id = ?",
                Integer.class, personId, personId
        );
        Integer debtCount = jdbcTemplate.queryForObject(
                "select count(1) from debts_header where person_id = ?",
                Integer.class, personId
        );
        return (txCount != null && txCount > 0) || (debtCount != null && debtCount > 0);
    }

    private void validateBusiness(Boolean isLegal, String companyName, String name, String lastName) {
        if (isLegal == null) throw new IllegalArgumentException("isLegal is required.");

        // Match CK_persons: legal => company_name not null; non-legal => company_name null :contentReference[oaicite:4]{index=4}
        if (isLegal) {
            if (companyName == null || companyName.trim().isEmpty()) {
                throw new IllegalArgumentException("For legal persons, companyName is required.");
            }
        } else {
            if (companyName != null && !companyName.trim().isEmpty()) {
                throw new IllegalArgumentException("For natural persons, companyName must be null/empty.");
            }
            // Practical rule (not DB): make sure at least one of name/lastName exists
            if ((name == null || name.trim().isEmpty()) && (lastName == null || lastName.trim().isEmpty())) {
                throw new IllegalArgumentException("For natural persons, at least name or lastName is required.");
            }
        }
    }

    private void apply(Person p,
                       String name,
                       String lastName,
                       String companyName,
                       String address,
                       String tel,
                       Boolean isLegal,
                       String dsc) {

        p.setName(trimToNull(name));
        p.setLastName(trimToNull(lastName));

        if (Boolean.TRUE.equals(isLegal)) {
            p.setCompanyName(Objects.requireNonNull(companyName).trim());
        } else {
            p.setCompanyName(null);
        }

        p.setAddress(trimToNull(address));
        p.setTel(trimToNull(tel));
        p.setLegal(isLegal);
        p.setDsc(trimToNull(dsc));
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private PersonResponse toResponse(Person p) {
        return new PersonResponse(
                p.getId(),
                p.getName(),
                p.getLastName(),
                p.getCompanyName(),
                p.getAddress(),
                p.getTel(),
                p.isLegal(),
                p.getDsc()
        );
    }
}
