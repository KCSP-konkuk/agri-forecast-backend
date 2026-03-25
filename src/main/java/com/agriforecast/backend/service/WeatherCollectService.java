package com.agriforecast.backend.service;

import com.agriforecast.backend.entity.WeatherData;
import com.agriforecast.backend.repository.WeatherDataRepository;
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
 * 기상청 ASOS API → WeatherData DB 저장
 * API: https://apihub.kma.go.kr/api/typ01/url/kma_sfcdd3.php
 * 일별 데이터 → 순별 평균 집계 후 저장
 *
 * 응답 형식: 텍스트(공백구분) 헤더 포함
 * 주요 컬럼: TM(일시), STN(지점), TA(평균기온), TX(최고기온), TN(최저기온),
 *            RN(강수량), HM(상대습도), SS(일조시간), SI(일사량)
 */
@Service
@Transactional
public class WeatherCollectService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherCollectService.class);
    private static final String BASE_URL =
            "https://apihub.kma.go.kr/api/typ01/url/kma_sfcdd3.php";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${weather.auth-key}")
    private String authKey;

    @Value("${weather.station}")
    private String station;

    private final WeatherDataRepository weatherDataRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public WeatherCollectService(WeatherDataRepository weatherDataRepository) {
        this.weatherDataRepository = weatherDataRepository;
    }

    /**
     * 특정 연월의 날씨 데이터 수집 및 저장
     */
    public int collectByYearMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        String tm1 = LocalDate.of(year, month, 1).format(DATE_FMT);
        String tm2 = LocalDate.of(year, month, ym.lengthOfMonth()).format(DATE_FMT);

        String rawData = fetchWeatherData(tm1, tm2);
        if (rawData == null || rawData.isBlank()) {
            logger.warn("기상청 데이터 없음 - {}/{}", year, month);
            return 0;
        }

        // 순별 집계용 맵
        Map<Integer, List<Double>> taByPeriod = new HashMap<>();
        Map<Integer, List<Double>> txByPeriod = new HashMap<>();
        Map<Integer, List<Double>> tnByPeriod = new HashMap<>();
        Map<Integer, List<Double>> rnByPeriod = new HashMap<>();
        Map<Integer, List<Double>> hmByPeriod = new HashMap<>();
        Map<Integer, List<Double>> siByPeriod = new HashMap<>();

        for (String line : rawData.split("\n")) {
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;

            String[] cols = line.split("\\s+");
            if (cols.length < 15) continue;

            try {
                // TM 형식: YYYYMMDD (cols[0])
                String tm = cols[0];
                int day = Integer.parseInt(tm.substring(6, 8));
                int periodType = getPeriodType(day);

                // 기상청 ASOS 일별 컬럼 순서 (kma_sfcdd3 기준):
                // 0:TM, 1:STN, 2:WD, 3:WS, 4:GST, 5:PA, 6:PS, 7:PT, 8:PR,
                // 9:RN(강수량), 10:RN_DAY, 11:TA(평균기온), 12:TD, 13:HM(상대습도),
                // 14:TN(최저기온), 15:TX(최고기온), 16:SS(일조), 17:SI(일사)
                addValue(taByPeriod, periodType, cols, 11);  // 평균기온
                addValue(txByPeriod, periodType, cols, 15);  // 최고기온
                addValue(tnByPeriod, periodType, cols, 14);  // 최저기온
                addValue(rnByPeriod, periodType, cols, 9);   // 강수량
                addValue(hmByPeriod, periodType, cols, 13);  // 상대습도
                addValue(siByPeriod, periodType, cols, 17);  // 일사량
            } catch (Exception e) {
                logger.debug("기상 데이터 파싱 skip: {}", line);
            }
        }

        int savedCount = 0;
        for (int periodType = 0; periodType <= 2; periodType++) {
            if (weatherDataRepository.findByYearAndMonthAndPeriodType(year, month, periodType).isPresent()) {
                continue;
            }

            Double avgTemp = average(taByPeriod.get(periodType));
            if (avgTemp == null) continue;

            WeatherData entity = new WeatherData();
            entity.setYear(year);
            entity.setMonth(month);
            entity.setPeriodType(periodType);
            entity.setAvgTemp(avgTemp);
            entity.setMaxTemp(average(txByPeriod.get(periodType)));
            entity.setMinTemp(average(tnByPeriod.get(periodType)));
            entity.setAvgRainfall(average(rnByPeriod.get(periodType)));
            entity.setAvgHumidity(average(hmByPeriod.get(periodType)));
            entity.setAvgSolar(average(siByPeriod.get(periodType)));
            weatherDataRepository.save(entity);
            savedCount++;
        }

        logger.info("기상 데이터 저장 완료 - {}/{}, {}건", year, month, savedCount);
        return savedCount;
    }

    private String fetchWeatherData(String tm1, String tm2) {
        try {
            String url = BASE_URL
                    + "?tm1=" + tm1
                    + "&tm2=" + tm2
                    + "&stn=" + station
                    + "&help=0"
                    + "&authKey=" + authKey;
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            logger.error("기상청 API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private void addValue(Map<Integer, List<Double>> map, int periodType, String[] cols, int idx) {
        if (idx >= cols.length) return;
        String val = cols[idx];
        if (val.equals("-9") || val.equals("-99") || val.equals("-999") || val.equals("null")) return;
        try {
            map.computeIfAbsent(periodType, k -> new ArrayList<>()).add(Double.parseDouble(val));
        } catch (NumberFormatException ignored) {}
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
