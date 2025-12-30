package com.app.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "debts_header")
public class DebtHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> persons(id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    // FK -> projects(id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "date_due", nullable = false)
    private LocalDate dateDue;

    @Column(name = "date_registered", nullable = false)
    private LocalDate dateRegistered;

    @Column(name = "dsc", length = 4000)
    private String dsc;

    public DebtHeader() {}

    public Long getId() { return id; }

    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public LocalDate getDateDue() { return dateDue; }
    public void setDateDue(LocalDate dateDue) { this.dateDue = dateDue; }

    public LocalDate getDateRegistered() { return dateRegistered; }
    public void setDateRegistered(LocalDate dateRegistered) { this.dateRegistered = dateRegistered; }

    public String getDsc() { return dsc; }
    public void setDsc(String dsc) { this.dsc = dsc; }
}
