package com.agriforecast.backend.service;

import com.agriforecast.backend.entity.CpiData;
import com.agriforecast.backend.entity.PpiData;
import com.agriforecast.backend.repository.CpiDataRepository;
import com.agriforecast.backend.repository.PpiDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * KOSIS API → CpiData, PpiData DB 저장
 * API: https://kosis.kr/openapi/statisticsData.do
 * 월별 데이터를 직접 저장 (집계 불필요)
 */
@Service
@Transactional
public class KosisService {

    private static final Logger logger = LoggerFactory.getLogger(KosisService.class);
    private static final String BASE_URL = "https://kosis.kr/openapi/statisticsData.do";

    @Value("${kosis.api-key}")
    private String apiKey;

    @Value("${kosis.cpi.org-id}")
    private String cpiOrgId;

    @Value("${kosis.cpi.tbl-id}")
    private String cpiTblId;

    @Value("${kosis.ppi.org-id}")
    private String ppiOrgId;

    @Value("${kosis.ppi.tbl-id}")
    private String ppiTblId;

    private final CpiDataRepository cpiDataRepository;
    private final PpiDataRepository ppiDataRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public KosisService(CpiDataRepository cpiDataRepository, PpiDataRepository ppiDataRepository) {
        this.cpiDataRepository = cpiDataRepository;
        this.ppiDataRepository = ppiDataRepository;
    }

    /**
     * 연도 범위로 CPI 수집 및 저장
     */
    public int collectCpi(int startYear, int endYear) {
        String startPrd = startYear + "01";
        String endPrd = endYear + "12";
        List<Map<String, Object>> items = fetchKosis(cpiOrgId, cpiTblId, startPrd, endPrd);
        if (items == null) return 0;

        int savedCount = 0;
        for (Map<String, Object> item : items) {
            try {
                String prd = String.valueOf(item.get("PRD_DE")); // e.g. "202401"
                int year = Integer.parseInt(prd.substring(0, 4));
                int month = Integer.parseInt(prd.substring(4, 6));
                double value = Double.parseDouble(String.valueOf(item.get("DT")));

                if (cpiDataRepository.findByYearAndMonth(year, month).isPresent()) continue;

                CpiData cpi = new CpiData();
                cpi.setYear(year);
                cpi.setMonth(month);
                cpi.setCpi(value);
                cpiDataRepository.save(cpi);
                savedCount++;
            } catch (Exception e) {
                logger.warn("CPI 데이터 파싱 실패: {}", item);
            }
        }
        logger.info("CPI 저장 완료 - {}~{}, {}건", startYear, endYear, savedCount);
        return savedCount;
    }

    /**
     * 연도 범위로 PPI 수집 및 저장
     */
    public int collectPpi(int startYear, int endYear) {
        String startPrd = startYear + "01";
        String endPrd = endYear + "12";
        List<Map<String, Object>> items = fetchKosis(ppiOrgId, ppiTblId, startPrd, endPrd);
        if (items == null) return 0;

        int savedCount = 0;
        for (Map<String, Object> item : items) {
            try {
                String prd = String.valueOf(item.get("PRD_DE"));
                int year = Integer.parseInt(prd.substring(0, 4));
                int month = Integer.parseInt(prd.substring(4, 6));
                double value = Double.parseDouble(String.valueOf(item.get("DT")));

                if (ppiDataRepository.findByYearAndMonth(year, month).isPresent()) continue;

                PpiData ppi = new PpiData();
                ppi.setYear(year);
                ppi.setMonth(month);
                ppi.setPpi(value);
                ppiDataRepository.save(ppi);
                savedCount++;
            } catch (Exception e) {
                logger.warn("PPI 데이터 파싱 실패: {}", item);
            }
        }
        logger.info("PPI 저장 완료 - {}~{}, {}건", startYear, endYear, savedCount);
        return savedCount;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchKosis(String orgId, String tblId, String startPrd, String endPrd) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("method", "getList")
                    .queryParam("apiKey", apiKey)
                    .queryParam("orgId", orgId)
                    .queryParam("tblId", tblId)
                    .queryParam("itmId", "T+")
                    .queryParam("objL1", "ALL")
                    .queryParam("format", "json")
                    .queryParam("jsonVD", "Y")
                    .queryParam("prdSe", "M")
                    .queryParam("startPrdDe", startPrd)
                    .queryParam("endPrdDe", endPrd)
                    .build().toUriString();

            Object response = restTemplate.getForObject(url, Object.class);
            if (response instanceof List) return (List<Map<String, Object>>) response;
        } catch (Exception e) {
            logger.error("KOSIS API 호출 실패 - orgId: {}, tblId: {}: {}", orgId, tblId, e.getMessage());
        }
        return null;
    }
}