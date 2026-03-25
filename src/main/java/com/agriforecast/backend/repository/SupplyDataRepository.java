package com.agriforecast.backend.repository;

import com.agriforecast.backend.entity.SupplyData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplyDataRepository extends JpaRepository<SupplyData, Integer> {

    Optional<SupplyData> findByItemNameAndYearAndMonthAndPeriodType(
            String itemName, Integer year, Integer month, Integer periodType);

    List<SupplyData> findByItemNameAndYearBetweenOrderByYearAscMonthAscPeriodTypeAsc(
            String itemName, Integer startYear, Integer endYear);
}