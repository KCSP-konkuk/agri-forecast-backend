package com.agriforecast.backend.repository;

import com.agriforecast.backend.entity.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeatherDataRepository extends JpaRepository<WeatherData, Integer> {

    Optional<WeatherData> findByYearAndMonthAndPeriodType(
            Integer year, Integer month, Integer periodType);

    List<WeatherData> findByYearBetweenOrderByYearAscMonthAscPeriodTypeAsc(
            Integer startYear, Integer endYear);
}