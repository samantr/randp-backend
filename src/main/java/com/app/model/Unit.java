package com.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "units")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 50, unique = true)
    private String title;

    @Column(name = "dsc", length = 5000)
    private String dsc;
}
