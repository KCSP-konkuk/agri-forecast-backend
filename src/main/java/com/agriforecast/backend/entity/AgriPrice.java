package com.agriforecast.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 농산물 도매 가격 데이터 (순별)
 * periodType: 0=상순, 1=중순, 2=하순
 */
@Entity
@Table(name = "agri_price", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ITEM_NAME", "YEAR", "MONTH", "PERIOD_TYPE"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgriPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ITEM_NAME", nullable = false, length = 50)
    private String itemName;

    @Column(name = "YEAR", nullable = false)
    private Integer year;

    @Column(name = "MONTH", nullable = false)
    private Integer month;

    // 0=상순, 1=중순, 2=하순
    @Column(name = "PERIOD_TYPE", nullable = false)
    private Integer periodType;

    @Column(name = "AVG_PRICE")
    private Double avgPrice;

    @Column(name = "PREV_YEAR_PRICE")
    private Double prevYearPrice;

    @Column(name = "NORMAL_PRICE")
    private Double normalPrice;
}