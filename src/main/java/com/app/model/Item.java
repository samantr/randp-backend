package com.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_category_id", nullable = false)
    private ItemCategory category;

    @Column(name = "code", nullable = false, length = 10, unique = true)
    private String code;

    @Column(name = "title", nullable = false, length = 50, unique = true)
    private String title;

    @Column(name = "dsc", length = 4000)
    private String dsc;
}
