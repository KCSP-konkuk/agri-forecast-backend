package com.agriforecast.backend.repository;

import com.agriforecast.backend.entity.OilPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OilPriceRepository extends JpaRepository<OilPrice, Integer> {

    Optional<OilPrice> findByYearAndMonthAndPeriodTypeAndOilTypeAndProduct(
            Integer year, Integer month, Integer periodType, String oilType, String product);

    List<OilPrice> findByOilTypeAndProductAndYearBetweenOrderByYearAscMonthAscPeriodTypeAsc(
            String oilType, String product, Integer startYear, Integer endYear);
}