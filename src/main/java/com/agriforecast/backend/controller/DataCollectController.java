package com.agriforecast.backend.controller;

import com.agriforecast.backend.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 외부 API 데이터 수집 컨트롤러
 * 모든 수집 작업은 POST 요청으로 수동 트리거
 */
@RestController
@RequestMapping("/api/collect")
@CrossOrigin(origins = "http://localhost:5173")
public class DataCollectController {

    private final KamisService kamisService;
    private final WeatherCollectService weatherCollectService;
    private final OilPriceCollectService oilPriceCollectService;
    private final KosisService kosisService;
    private final ExchangeRateCollectService exchangeRateCollectService;

    public DataCollectController(KamisService kamisService,
                                  WeatherCollectService weatherCollectService,
                                  OilPriceCollectService oilPriceCollectService,
                                  KosisService kosisService,
                                  ExchangeRateCollectService exchangeRateCollectService) {
        this.kamisService = kamisService;
        this.weatherCollectService = weatherCollectService;
        this.oilPriceCollectService = oilPriceCollectService;
        this.kosisService = kosisService;
        this.exchangeRateCollectService = exchangeRateCollectService;
    }

    /**
     * 특정 연월 전체 데이터 일괄 수집
     * POST /api/collect/all?year=2024&month=1
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> collectAll(
            @RequestParam int year,
            @RequestParam int month) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            result.put("kamis_price", kamisService.collectPriceByYearMonth(year, month));
            result.put("kamis_supply", kamisService.collectSupplyByYearMonth(year, month));
            result.put("weather", weatherCollectService.collectByYearMonth(year, month));
            result.put("oil_price", oilPriceCollectService.collectByYearMonth(year, month));
            result.put("exchange_rate", exchangeRateCollectService.collectByYearMonth(year, month));
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * KAMIS 가격 수집 (양파)
     * POST /api/collect/kamis/price?year=2024&month=1
     */
    @PostMapping("/kamis/price")
    public ResponseEntity<Map<String, Object>> collectKamisPrice(
            @RequestParam int year, @RequestParam int month) {
        int saved = kamisService.collectPriceByYearMonth(year, month);
        return ResponseEntity.ok(Map.of("saved", saved, "year", year, "month", month));
    }

    /**
     * KAMIS 반입량 수집 (양파)
     * POST /api/collect/kamis/supply?year=2024&month=1
     */
    @PostMapping("/kamis/supply")
    public ResponseEntity<Map<String, Object>> collectKamisSupply(
            @RequestParam int year, @RequestParam int month) {
        int saved = kamisService.collectSupplyByYearMonth(year, month);
        return ResponseEntity.ok(Map.of("saved", saved, "year", year, "month", month));
    }

    /**
     * 기상청 날씨 수집 (무안 164)
     * POST /api/collect/weather?year=2024&month=1
     */
    @PostMapping("/weather")
    public ResponseEntity<Map<String, Object>> collectWeather(
            @RequestParam int year, @RequestParam int month) {
        int saved = weatherCollectService.collectByYearMonth(year, month);
        return ResponseEntity.ok(Map.of("saved", saved, "year", year, "month", month));
    }

    /**
     * 유가 수집 (국내+국제)
     * POST /api/collect/oil?year=2024&month=1
     */
    @PostMapping("/oil")
    public ResponseEntity<Map<String, Object>> collectOil(
            @RequestParam int year, @RequestParam int month) {
        int saved = oilPriceCollectService.collectByYearMonth(year, month);
        return ResponseEntity.ok(Map.of("saved", saved, "year", year, "month", month));
    }

    /**
     * 환율 수집
     * POST /api/collect/exchange?year=2024&month=1
     */
    @PostMapping("/exchange")
    public ResponseEntity<Map<String, Object>> collectExchange(
            @RequestParam int year, @RequestParam int month) {
        int saved = exchangeRateCollectService.collectByYearMonth(year, month);
        return ResponseEntity.ok(Map.of("saved", saved, "year", year, "month", month));
    }

    /**
     * CPI 수집 (연도 범위)
     * POST /api/collect/cpi?startYear=2018&endYear=2025
     */
    @PostMapping("/cpi")
    public ResponseEntity<Map<String, Object>> collectCpi(
            @RequestParam int startYear, @RequestParam int endYear) {
        int saved = kosisService.collectCpi(startYear, endYear);
        return ResponseEntity.ok(Map.of("saved", saved, "startYear", startYear, "endYear", endYear));
    }

    /**
     * PPI 수집 (연도 범위)
     * POST /api/collect/ppi?startYear=2018&endYear=2025
     */
    @PostMapping("/ppi")
    public ResponseEntity<Map<String, Object>> collectPpi(
            @RequestParam int startYear, @RequestParam int endYear) {
        int saved = kosisService.collectPpi(startYear, endYear);
        return ResponseEntity.ok(Map.of("saved", saved, "startYear", startYear, "endYear", endYear));
    }
}