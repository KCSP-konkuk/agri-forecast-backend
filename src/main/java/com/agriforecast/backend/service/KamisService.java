package com.agriforecast.backend.service;

import com.agriforecast.backend.entity.AgriPrice;
import com.agriforecast.backend.entity.SupplyData;
import com.agriforecast.backend.repository.AgriPriceRepository;
import com.agriforecast.backend.repository.SupplyDataRepository;
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
 * KAMIS API → AgriPrice(가격), SupplyData(반입량) DB 저장
 * API: https://www.kamis.or.kr/service/price/xml.do
 * 일별 데이터 → 순별 평균 집계 후 저장
 *
 * p_cert_id: KAMIS 가입 계정의 인증 ID (application.properties에서 수정)
 */
@Service
@Transactional
public class KamisService {

    private static final Logger logger = LoggerFactory.getLogger(KamisService.class);
    private static final String BASE_URL = "https://www.kamis.or.kr/service/price/xml.do";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 양파: 품목분류코드=200, 품목코드=225
    private static final String ITEM_NAME = "양파";
    private static final String ITEM_CATEGORY_CODE = "200";
    private static final String ITEM_CODE = "225";
    // 1101=서울 가락시장 (주요 기준 도매시장)
    private static final String COUNTRY_CODE = "1101";

    @Value("${kamis.cert-key}")
    private String certKey;

    @Value("${kamis.cert-id}")
    private String certId;

    private final AgriPriceRepository agriPriceRepository;
    private final SupplyDataRepository supplyDataRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public KamisService(AgriPriceRepository agriPriceRepository,
                        SupplyDataRepository supplyDataRepository) {
        this.agriPriceRepository = agriPriceRepository;
        this.supplyDataRepository = supplyDataRepository;
    }

    /**
     * 특정 연월의 양파 가격 데이터 수집 및 저장
     */
    public int collectPriceByYearMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        String startDay = LocalDate.of(year, month, 1).format(DATE_FMT);
        String endDay = LocalDate.of(year, month, ym.lengthOfMonth()).format(DATE_FMT);

        List<Map<String, Object>> items = fetchPriceList(startDay, endDay);
        if (items == null || items.isEmpty()) {
            logger.warn("KAMIS 가격 데이터 없음 - {}/{}", year, month);
            return 0;
        }

        // 순별로 가격 집계
        Map<Integer, List<Double>> priceByPeriod = new HashMap<>();
        for (Map<String, Object> item : items) {
            try {
                String regday = String.valueOf(item.get("regday")); // e.g. "2024/01/03" or "01/03"
                int day = extractDay(regday);
                String priceStr = String.valueOf(item.get("price")).replace(",", "").trim();
                if (priceStr.equals("-") || priceStr.isEmpty()) continue;

                double price = Double.parseDouble(priceStr);
                int periodType = getPeriodType(day);
                priceByPeriod.computeIfAbsent(periodType, k -> new ArrayList<>()).add(price);
            } catch (Exception e) {
                logger.debug("KAMIS 가격 파싱 skip: {}", item);
            }
        }

        int savedCount = 0;
        for (int periodType = 0; periodType <= 2; periodType++) {
            if (agriPriceRepository.findByItemNameAndYearAndMonthAndPeriodType(
                    ITEM_NAME, year, month, periodType).isPresent()) continue;

            Double avgPrice = average(priceByPeriod.get(periodType));
            if (avgPrice == null) continue;

            AgriPrice entity = new AgriPrice();
            entity.setItemName(ITEM_NAME);
            entity.setYear(year);
            entity.setMonth(month);
            entity.setPeriodType(periodType);
            entity.setAvgPrice(avgPrice);
            agriPriceRepository.save(entity);
            savedCount++;
        }

        logger.info("KAMIS 가격 저장 완료 - {}/{}, {}건", year, month, savedCount);
        return savedCount;
    }

    /**
     * 특정 연월의 양파 반입량 데이터 수집 및 저장
     */
    public int collectSupplyByYearMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        String startDay = LocalDate.of(year, month, 1).format(DATE_FMT);
        String endDay = LocalDate.of(year, month, ym.lengthOfMonth()).format(DATE_FMT);

        List<Map<String, Object>> items = fetchSupplyList(startDay, endDay);
        if (items == null || items.isEmpty()) {
            logger.warn("KAMIS 반입량 데이터 없음 - {}/{}", year, month);
            return 0;
        }

        Map<Integer, List<Double>> supplyByPeriod = new HashMap<>();
        for (Map<String, Object> item : items) {
            try {
                String regday = String.valueOf(item.get("regday"));
                int day = extractDay(regday);
                String supplyStr = String.valueOf(item.get("totAmt")).replace(",", "").trim();
                if (supplyStr.equals("-") || supplyStr.isEmpty()) continue;

                double supply = Double.parseDouble(supplyStr);
                int periodType = getPeriodType(day);
                supplyByPeriod.computeIfAbsent(periodType, k -> new ArrayList<>()).add(supply);
            } catch (Exception e) {
                logger.debug("KAMIS 반입량 파싱 skip: {}", item);
            }
        }

        int savedCount = 0;
        for (int periodType = 0; periodType <= 2; periodType++) {
            if (supplyDataRepository.findByItemNameAndYearAndMonthAndPeriodType(
                    ITEM_NAME, year, month, periodType).isPresent()) continue;

            Double totalSupply = sum(supplyByPeriod.get(periodType));
            if (totalSupply == null) continue;

            SupplyData entity = new SupplyData();
            entity.setItemName(ITEM_NAME);
            entity.setYear(year);
            entity.setMonth(month);
            entity.setPeriodType(periodType);
            entity.setTotalSupply(totalSupply);
            supplyDataRepository.save(entity);
            savedCount++;
        }

        logger.info("KAMIS 반입량 저장 완료 - {}/{}, {}건", year, month, savedCount);
        return savedCount;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchPriceList(String startDay, String endDay) {
        try {
            String url = BASE_URL
                    + "?action=periodProductList"
                    + "&p_cert_key=" + certKey
                    + "&p_cert_id=" + certId
                    + "&p_returntype=json"
                    + "&p_startday=" + startDay
                    + "&p_endday=" + endDay
                    + "&p_productclscode=01"
                    + "&p_itemcategorycode=" + ITEM_CATEGORY_CODE
                    + "&p_itemcode=" + ITEM_CODE
                    + "&p_kindcode="
                    + "&p_graderank=05"
                    + "&p_countrycode=" + COUNTRY_CODE;

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return null;

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) return null;

            Object items = data.get("item");
            if (items instanceof List) return (List<Map<String, Object>>) items;
        } catch (Exception e) {
            logger.error("KAMIS 가격 API 호출 실패: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchSupplyList(String startDay, String endDay) {
        try {
            // 도매시장 반입량 API (action=dailySalesList)
            String url = BASE_URL
                    + "?action=dailySalesList"
                    + "&p_cert_key=" + certKey
                    + "&p_cert_id=" + certId
                    + "&p_returntype=json"
                    + "&p_startday=" + startDay
                    + "&p_endday=" + endDay
                    + "&p_itemcategorycode=" + ITEM_CATEGORY_CODE
                    + "&p_itemcode=" + ITEM_CODE
                    + "&p_countrycode=" + COUNTRY_CODE;

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return null;

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) return null;

            Object items = data.get("item");
            if (items instanceof List) return (List<Map<String, Object>>) items;
        } catch (Exception e) {
            logger.error("KAMIS 반입량 API 호출 실패: {}", e.getMessage());
        }
        return null;
    }

    // "2024/01/03" 또는 "01/03" 형식에서 일(day) 추출
    private int extractDay(String regday) {
        String[] parts = regday.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
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

    private Double sum(List<Double> values) {
        if (values == null || values.isEmpty()) return null;
        return values.stream().mapToDouble(Double::doubleValue).sum();
    }
}