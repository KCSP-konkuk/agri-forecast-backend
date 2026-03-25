package com.agriforecast.backend.repository;

import com.agriforecast.backend.entity.PpiData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PpiDataRepository extends JpaRepository<PpiData, Integer> {

    Optional<PpiData> findByYearAndMonth(Integer year, Integer month);

    List<PpiData> findByYearBetweenOrderByYearAscMonthAsc(Integer startYear, Integer endYear);
}
