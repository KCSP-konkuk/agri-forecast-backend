package com.agriforecast.backend.repository;

import com.agriforecast.backend.entity.AgriPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgriPriceRepository extends JpaRepository<AgriPrice, Integer> {

    Optional<AgriPrice> findByItemNameAndYearAndMonthAndPeriodType(
            String itemName, Integer year, Integer month, Integer periodType);

    List<AgriPrice> findByItemNameAndYearBetweenOrderByYearAscMonthAscPeriodTypeAsc(
            String itemName, Integer startYear, Integer endYear);

    List<AgriPrice> findByItemNameOrderByYearAscMonthAscPeriodTypeAsc(String itemName);
}