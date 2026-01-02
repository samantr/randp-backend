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
        if (req == null) throw new IllegalArgumentException("اطلاعات شخص ارسال نشده است.");

        validateBusiness(req.isLegal(), req.companyName(), req.name(), req.lastName());

        if (Boolean.TRUE.equals(req.isLegal())) {
            String company = req.companyName().trim();
            if (personRepository.existsByIsLegalTrueAndCompanyNameIgnoreCase(company)) {
                throw new IllegalArgumentException("این شرکت/سازمان قبلاً ثبت شده است.");
            }
        } else {
            if (personRepository.existsNatural(req.name(), req.lastName())) {
                throw new IllegalArgumentException("این شخص قبلاً ثبت شده است.");
            }
        }

        Person p = new Person();
        apply(p, req.name(), req.lastName(), req.companyName(), req.address(), req.tel(), req.isLegal(), req.dsc());

        try {
            Person saved = personRepository.save(p);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("ثبت شخص انجام نشد. لطفاً اطلاعات را بررسی کنید.");
        }
    }

    @Transactional(readOnly = true)
    public Page<PersonResponse> getAll(Pageable pageable) {
        return personRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PersonResponse getById(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه شخص الزامی است.");

        return toResponse(personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("شخص یافت نشد. (شناسه: " + id + ")")));
    }

    @Transactional
    public PersonResponse update(Long id, PersonUpdateRequest req) {
        if (id == null) throw new IllegalArgumentException("شناسه شخص الزامی است.");
        if (req == null) throw new IllegalArgumentException("اطلاعات ویرایش شخص ارسال نشده است.");

        validateBusiness(req.isLegal(), req.companyName(), req.name(), req.lastName());

        Person p = personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("شخص یافت نشد. (شناسه: " + id + ")"));

        if (Boolean.TRUE.equals(req.isLegal())) {
            String company = req.companyName().trim();
            if (personRepository.existsByIsLegalTrueAndCompanyNameIgnoreCaseAndIdNot(company, id)) {
                throw new IllegalArgumentException("این شرکت/سازمان قبلاً ثبت شده است.");
            }
        } else {
            if (personRepository.existsNaturalExcludingId(req.name(), req.lastName(), id)) {
                throw new IllegalArgumentException("این شخص قبلاً ثبت شده است.");
            }
        }

        apply(p, req.name(), req.lastName(), req.companyName(), req.address(), req.tel(), req.isLegal(), req.dsc());

        try {
            Person saved = personRepository.save(p);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("ویرایش شخص انجام نشد. لطفاً اطلاعات را بررسی کنید.");
        }
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("شناسه شخص الزامی است.");

        Person p = personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("شخص یافت نشد. (شناسه: " + id + ")"));

        if (isPersonReferenced(id)) {
            throw new IllegalArgumentException("امکان حذف شخص وجود ندارد؛ برای این شخص بدهی یا پرداخت ثبت شده است.");
        }

        personRepository.delete(p);
    }

    @Transactional(readOnly = true)
    public Page<PersonResponse> search(String q, Pageable pageable) {
        return personRepository.search(q, pageable).map(this::toResponse);
    }

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
        if (isLegal == null) throw new IllegalArgumentException("نوع شخص (حقیقی/حقوقی) الزامی است.");

        if (isLegal) {
            if (companyName == null || companyName.trim().isEmpty()) {
                throw new IllegalArgumentException("برای شخص حقوقی، نام شرکت/سازمان الزامی است.");
            }
        } else {
            if (companyName != null && !companyName.trim().isEmpty()) {
                throw new IllegalArgumentException("برای شخص حقیقی، نام شرکت/سازمان نباید وارد شود.");
            }
            if ((name == null || name.trim().isEmpty()) && (lastName == null || lastName.trim().isEmpty())) {
                throw new IllegalArgumentException("برای شخص حقیقی، حداقل نام یا نام‌خانوادگی الزامی است.");
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
