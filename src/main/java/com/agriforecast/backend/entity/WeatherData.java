package com.agriforecast.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 기상 데이터 (순별)
 * periodType: 0=상순, 1=중순, 2=하순
 */
@Entity
@Table(name = "weather_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"YEAR", "MONTH", "PERIOD_TYPE"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeatherData {

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

    @Column(name = "AVG_TEMP")
    private Double avgTemp;

    @Column(name = "MAX_TEMP")
    private Double maxTemp;

    @Column(name = "MIN_TEMP")
    private Double minTemp;

    @Column(name = "AVG_RAINFALL")
    private Double avgRainfall;

    @Column(name = "AVG_SOLAR")
    private Double avgSolar;

    @Column(name = "AVG_HUMIDITY")
    private Double avgHumidity;

    // 전년 동기 데이터
    @Column(name = "PREV_YEAR_TEMP")
    private Double prevYearTemp;

    @Column(name = "PREV_YEAR_RAINFALL")
    private Double prevYearRainfall;

    @Column(name = "PREV_YEAR_SOLAR")
    private Double prevYearSolar;

    @Column(name = "PREV_YEAR_HUMIDITY")
    private Double prevYearHumidity;
}
