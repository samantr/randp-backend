package com.app.controller;

import com.app.dto.person.PersonCreateRequest;
import com.app.dto.person.PersonResponse;
import com.app.dto.person.PersonUpdateRequest;
import com.app.service.PersonService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/persons")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @PostMapping
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personService.create(req));
    }

    @GetMapping
    public ResponseEntity<Page<PersonResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(personService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(personService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody PersonUpdateRequest req) {
        return ResponseEntity.ok(personService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        personService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Extra endpoint: search
    @GetMapping("/search")
    public ResponseEntity<Page<PersonResponse>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(personService.search(q, pageable));
    }
}
