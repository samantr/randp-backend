package com.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "item_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 50, unique = true)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ItemCategory parent;

    @Column(name = "dsc", length = 4000)
    private String dsc;
}
