package com.agriforecast.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 소비자물가지수 (월별)
 */
@Entity
@Table(name = "cpi_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"YEAR", "MONTH"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CpiData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "YEAR", nullable = false)
    private Integer year;

    @Column(name = "MONTH", nullable = false)
    private Integer month;

    @Column(name = "CPI", nullable = false)
    private Double cpi;
}