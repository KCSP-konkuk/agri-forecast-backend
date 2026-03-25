package com.agriforecast.backend.service;

import com.agriforecast.backend.entity.OilPrice;
import com.agriforecast.backend.repository.OilPriceRepository;
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
 * 오피넷 API → OilPrice DB 저장
 *
 * [국내유가] 오피넷 전국 평균 소매가
 * - 휘발유(B034), 경유(D047)
 * API: https://www.opinet.co.kr/api/avgAllOilPrice.do
 *
 * [국제유가] 오피넷 국제 원유가격
 * - 두바이유(CRD_OIL_D), WTI(CRD_OIL_W)
 * API: https://www.opinet.co.kr/api/internationalOilPriceList.do
 *
 * 일별 데이터 → 순별 평균 집계 후 저장
 */
@Service
@Transactional
public class OilPriceCollectService {

    private static final Logger logger = LoggerFactory.getLogger(OilPriceCollectService.class);
    private static final String DOMESTIC_URL =
            "https://www.opinet.co.kr/api/avgAllOilPrice.do?code={key}&out=json&date={date}";
    private static final String INTERNATIONAL_URL =
            "https://www.opinet.co.kr/api/internationalOilPriceList.do?code={key}&out=json&date={date}";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static final String DOMESTIC = "DOMESTIC";
    public static final String INTERNATIONAL = "INTERNATIONAL";

    @Value("${opinet.api-key}")
    private String apiKey;

    private final OilPriceRepository oilPriceRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public OilPriceCollectService(OilPriceRepository oilPriceRepository) {
        this.oilPriceRepository = oilPriceRepository;
    }

    /**
     * 특정 연월의 유가 데이터 수집 및 저장 (국내 + 국제)
     */
    public int collectByYearMonth(int year, int month) {
        int saved = 0;
        saved += collectDomestic(year, month);
        saved += collectInternational(year, month);
        return saved;
    }

    private int collectDomestic(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        // 제품별 순별 집계 맵
        Map<String, Map<Integer, List<Double>>> priceMap = new HashMap<>();
        priceMap.put("휘발유", new HashMap<>());
        priceMap.put("경유", new HashMap<>());

        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            String dateStr = LocalDate.of(year, month, day).format(DATE_FMT);
            List<Map<String, Object>> items = fetchJson(DOMESTIC_URL, dateStr);
            if (items == null) continue;

            int periodType = getPeriodType(day);
            for (Map<String, Object> item : items) {
                String prodCode = String.valueOf(item.get("PRODCD"));
                String priceStr = String.valueOf(item.get("PRICE")).replace(",", "");
                if (priceStr.equals("null") || priceStr.isEmpty()) continue;

                String product = null;
                if ("B034".equals(prodCode)) product = "휘발유";
                else if ("D047".equals(prodCode)) product = "경유";
                if (product == null) continue;

                try {
                    double price = Double.parseDouble(priceStr);
                    priceMap.get(product).computeIfAbsent(periodType, k -> new ArrayList<>()).add(price);
                } catch (NumberFormatException ignored) {}
            }
        }

        return savePeriodData(year, month, priceMap, DOMESTIC);
    }

    private int collectInternational(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        Map<String, Map<Integer, List<Double>>> priceMap = new HashMap<>();
        priceMap.put("DUBAI", new HashMap<>());
        priceMap.put("WTI", new HashMap<>());

        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            String dateStr = LocalDate.of(year, month, day).format(DATE_FMT);
            List<Map<String, Object>> items = fetchJson(INTERNATIONAL_URL, dateStr);
            if (items == null) continue;

            int periodType = getPeriodType(day);
            for (Map<String, Object> item : items) {
                String prodCode = String.valueOf(item.get("PRODCD"));
                String priceStr = String.valueOf(item.get("PRICE")).replace(",", "");
                if (priceStr.equals("null") || priceStr.isEmpty()) continue;

                String product = null;
                if (prodCode.contains("D")) product = "DUBAI";
                else if (prodCode.contains("W")) product = "WTI";
                if (product == null) continue;

                try {
                    double price = Double.parseDouble(priceStr);
                    priceMap.get(product).computeIfAbsent(periodType, k -> new ArrayList<>()).add(price);
                } catch (NumberFormatException ignored) {}
            }
        }

        return savePeriodData(year, month, priceMap, INTERNATIONAL);
    }

    private int savePeriodData(int year, int month,
                                Map<String, Map<Integer, List<Double>>> priceMap, String oilType) {
        int savedCount = 0;
        for (Map.Entry<String, Map<Integer, List<Double>>> entry : priceMap.entrySet()) {
            String product = entry.getKey();
            for (int periodType = 0; periodType <= 2; periodType++) {
                if (oilPriceRepository.findByYearAndMonthAndPeriodTypeAndOilTypeAndProduct(
                        year, month, periodType, oilType, product).isPresent()) continue;

                Double avg = average(entry.getValue().get(periodType));
                if (avg == null) continue;

                OilPrice entity = new OilPrice();
                entity.setYear(year);
                entity.setMonth(month);
                entity.setPeriodType(periodType);
                entity.setOilType(oilType);
                entity.setProduct(product);
                entity.setClosePrice(avg);
                oilPriceRepository.save(entity);
                savedCount++;
            }
        }
        logger.info("유가({}) 저장 완료 - {}/{}, {}건", oilType, year, month, savedCount);
        return savedCount;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchJson(String urlTemplate, String dateStr) {
        try {
            String url = urlTemplate.replace("{key}", apiKey).replace("{date}", dateStr);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return null;

            Object result = response.get("result");
            if (result instanceof Map) {
                Object oil = ((Map<?, ?>) result).get("OIL");
                if (oil instanceof List) return (List<Map<String, Object>>) oil;
            }
        } catch (Exception e) {
            logger.debug("오피넷 API 호출 실패 - {}: {}", dateStr, e.getMessage());
        }
        return null;
    }

    private int getPeriodType(int day) {
        if (day <= 10) return 0;
        else if (day <= 20) return 1;
        else return 2;
    }

    private Double average(List<Double> values) {
        if (values == null || values.isEmpty()) return null;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}