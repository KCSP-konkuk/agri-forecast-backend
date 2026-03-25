package com.agriforecast.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 유가 데이터 (순별)
 * periodType: 0=상순, 1=중순, 2=하순
 * oilType: DOMESTIC(국내), INTERNATIONAL(국제)
 * product: 휘발유, 경유, DUBAI, WTI
 */
@Entity
@Table(name = "oil_price", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"YEAR", "MONTH", "PERIOD_TYPE", "OIL_TYPE", "PRODUCT"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OilPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "YEAR", nullable = false)
    private Integer year;

    @Column(name = "MONTH", nullable = false)
    private Integer month;

    // 0=상순, 1=중순, 2=하순
    @Column(name = "PERIOD_TYPE", nullable = false)
    private Integer periodType;

    // DOMESTIC / INTERNATIONAL
    @Column(name = "OIL_TYPE", nullable = false, length = 20)
    private String oilType;

    // 휘발유, 경유, DUBAI, WTI 등
    @Column(name = "PRODUCT", nullable = false, length = 20)
    private String product;

    @Column(name = "CLOSE_PRICE")
    private Double closePrice;

    @Column(name = "OPEN_PRICE")
    private Double openPrice;

    @Column(name = "HIGH_PRICE")
    private Double highPrice;

    @Column(name = "LOW_PRICE")
    private Double lowPrice;
}
