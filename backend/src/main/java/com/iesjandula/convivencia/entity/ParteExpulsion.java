package com.iesjandula.convivencia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parte_expulsion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParteExpulsion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parte_id", nullable = false)
    private ParteDisciplinario parte;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "expulsion_id", nullable = false)
    private Expulsion expulsion;
}
