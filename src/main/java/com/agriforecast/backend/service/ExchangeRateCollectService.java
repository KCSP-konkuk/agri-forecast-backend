package com.agriforecast.backend.service;

import com.agriforecast.backend.entity.ExchangeRate;
import com.agriforecast.backend.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 한국수출입은행 환율 API → ExchangeRate DB 저장
 * API: GET https://www.koreaexim.go.kr/site/program/financial/exchangeJSON
 * 응답: 일별 환율 → 순별 평균 집계 후 저장
 */
@Service
@Transactional
public class ExchangeRateCollectService {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateCollectService.class);
    private static final String API_URL =
            "https://www.koreaexim.go.kr/site/program/financial/exchangeJSON?authkey={authkey}&searchdate={date}&data=AP01";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${koreaexim.auth-key}")
    private String authKey;

    private final ExchangeRateRepository exchangeRateRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public ExchangeRateCollectService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    /**
     * 특정 연월의 환율 데이터 수집 및 저장
     */
    public int collectByYearMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        Map<Integer, List<Double>> usdByPeriod = new HashMap<>();
        Map<Integer, List<Double>> cnyByPeriod = new HashMap<>();
        Map<Integer, List<Double>> usdChangeByPeriod = new HashMap<>();
        Map<Integer, List<Double>> cnyChangeByPeriod = new HashMap<>();

        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = LocalDate.of(year, month, day);
            if (date.getDayOfWeek().getValue() >= 6) continue; // 주말 skip

            List<Map<String, Object>> rates = fetchRates(date);
            if (rates == null) continue;

            int periodType = getPeriodType(day);
            Double usd = extractRate(rates, "USD");
            Double cny = extractRate(rates, "CNY");

            if (usd != null) {
                usdByPeriod.computeIfAbsent(periodType, k -> new ArrayList<>()).add(usd);
            }
            if (cny != null) {
                cnyByPeriod.computeIfAbsent(periodType, k -> new ArrayList<>()).add(cny);
            }
        }

        int savedCount = 0;
        for (int periodType = 0; periodType <= 2; periodType++) {
            if (exchangeRateRepository.findByYearAndMonthAndPeriodType(year, month, periodType).isPresent()) {
                continue;
            }

            Double usdAvg = average(usdByPeriod.get(periodType));
            Double cnyAvg = average(cnyByPeriod.get(periodType));
            if (usdAvg == null && cnyAvg == null) continue;

            ExchangeRate entity = new ExchangeRate();
            entity.setYear(year);
            entity.setMonth(month);
            entity.setPeriodType(periodType);
            entity.setUsdKrw(usdAvg);
            entity.setCnyKrw(cnyAvg);
            exchangeRateRepository.save(entity);
            savedCount++;
        }

        logger.info("환율 저장 완료 - {}/{}, {}건", year, month, savedCount);
        return savedCount;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchRates(LocalDate date) {
        try {
            Map<String, String> params = Map.of("authkey", authKey, "date", date.format(DATE_FMT));
            Object[] response = restTemplate.getForObject(API_URL, Object[].class, params);
            if (response == null) return null;
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : response) {
                if (item instanceof Map) result.add((Map<String, Object>) item);
            }
            return result;
        } catch (Exception e) {
            logger.warn("환율 API 호출 실패 - {}: {}", date, e.getMessage());
            return null;
        }
    }

    private Double extractRate(List<Map<String, Object>> rates, String currency) {
        return rates.stream()
                .filter(r -> currency.equals(r.get("cur_unit")))
                .map(r -> {
                    String val = String.valueOf(r.get("deal_bas_r")).replace(",", "");
                    try { return Double.parseDouble(val); } catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Double average(List<Double> values) {
        if (values == null || values.isEmpty()) return null;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private int getPeriodType(int day) {
        if (day <= 10) return 0;
        else if (day <= 20) return 1;
        else return 2;
    }
}