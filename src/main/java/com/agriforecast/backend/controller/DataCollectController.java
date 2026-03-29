package com.agriforecast.backend.controller;

import com.agriforecast.backend.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    private final NongnetService nongnetService;
    private final WeatherCollectService weatherCollectService;
    private final OilPriceCollectService oilPriceCollectService;
    private final KosisService kosisService;
    private final ExchangeRateCollectService exchangeRateCollectService;
    private final CsvImportService csvImportService;

    public DataCollectController(NongnetService nongnetService,
                                  WeatherCollectService weatherCollectService,
                                  OilPriceCollectService oilPriceCollectService,
                                  KosisService kosisService,
                                  ExchangeRateCollectService exchangeRateCollectService,
                                  CsvImportService csvImportService) {
        this.nongnetService = nongnetService;
        this.weatherCollectService = weatherCollectService;
        this.oilPriceCollectService = oilPriceCollectService;
        this.kosisService = kosisService;
        this.exchangeRateCollectService = exchangeRateCollectService;
        this.csvImportService = csvImportService;
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
            result.put("nongnet_price", nongnetService.collectPriceByYearMonth(year, month));
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
     * 농넷(Nongnet) 가격 크롤링 (양파/배추)
     * POST /api/collect/nongnet/price?year=2024&month=1
     */
    @PostMapping("/nongnet/price")
    public ResponseEntity<Map<String, Object>> collectNongnetPrice(
            @RequestParam int year, @RequestParam int month) {
        int saved = nongnetService.collectPriceByYearMonth(year, month);
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
     * 환율 일별 수집 - 오늘
     * POST /api/collect/exchange/daily
     */
    @PostMapping("/exchange/daily")
    public ResponseEntity<Map<String, Object>> collectExchangeToday() {
        boolean saved = exchangeRateCollectService.collectToday();
        return ResponseEntity.ok(Map.of("saved", saved, "date", LocalDate.now().toString()));
    }

    /**
     * 환율 일별 수집 - 날짜 범위 (과거 데이터 채우기용)
     * POST /api/collect/exchange/daily/range?startDate=2024-01-01&endDate=2024-12-31
     */
    @PostMapping("/exchange/daily/range")
    public ResponseEntity<Map<String, Object>> collectExchangeDailyRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        int saved = exchangeRateCollectService.collectDailyRange(startDate, endDate);
        return ResponseEntity.ok(Map.of("saved", saved,
                "startDate", startDate.toString(), "endDate", endDate.toString()));
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

    /**
     * 로컬 CSV 데이터 일괄 삽입 (과거 기록 채우기용)
     * POST /api/collect/csv?filePath=c:\Users\shm87\OneDrive\바탕 화면\졸업프로젝트\agri-forecast-backend\src\test\java\양파3_22.csv
     */
    @PostMapping("/csv")
    public ResponseEntity<Map<String, Object>> importCsv(
            @RequestParam String filePath) {
        try {
            int saved = csvImportService.importAgriPriceCsv(filePath);
            return ResponseEntity.ok(Map.of("status", "success", "savedCount", saved, "filePath", filePath));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}