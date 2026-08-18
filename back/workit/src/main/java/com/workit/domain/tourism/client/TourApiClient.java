package com.workit.domain.tourism.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.tourism.exception.TourismErrorCode;
import com.workit.domain.tourism.vo.TourApiItem;
import com.workit.domain.tourism.vo.TourApiDetail;
import com.workit.domain.tourism.vo.TourApiPage;
import com.workit.domain.tourism.vo.TourismCategory;
import com.workit.domain.tourism.vo.TourismPlaceType;
import com.workit.domain.tourism.vo.TourismTargetRegion;
import com.workit.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TourApiClient {
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:0\\d{1,3}[-\\s)]*\\d{3,4}[-\\s]*\\d{4}"
                    + "|1\\d{3}[-\\s]*\\d{4})(?!\\d)");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;
    private final String serviceKey;
    private final String mobileApp;
    private final int pageSize;

    public TourApiClient(
            @Qualifier("tourismRestTemplate") RestTemplate restTemplate,
            @Value("${tourism.api.base-url}") String baseUrl,
            @Value("${tourism.api.service-key}") String serviceKey,
            @Value("${tourism.api.mobile-app}") String mobileApp,
            @Value("${tourism.api.page-size}") int pageSize) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.serviceKey = normalizeServiceKey(serviceKey);
        this.mobileApp = mobileApp;
        this.pageSize = Math.max(1, Math.min(pageSize, 1000));
    }

    public boolean isConfigured() {
        return StringUtils.hasText(serviceKey);
    }

    public TourApiPage fetch(TourismCategory category, TourismTargetRegion targetRegion,
                             int pageNo, boolean active, String modifiedDate, Long syncId) {
        return fetch(category.getContentTypeId(), category.getClassification1(),
                category.getClassification2(), category, TourismPlaceType.ACTIVITY,
                targetRegion, pageNo, active, modifiedDate, syncId);
    }

    public TourApiPage fetch(TourismPlaceType placeType, TourismTargetRegion targetRegion,
                             int pageNo, boolean active, String modifiedDate, Long syncId) {
        if (placeType == TourismPlaceType.ACTIVITY) {
            throw new IllegalArgumentException("ACTIVITY는 TourismCategory로 조회해야 합니다.");
        }
        int contentTypeId = placeType == TourismPlaceType.RESTAURANT ? 39 : 32;
        return fetch(contentTypeId, null, null, null, placeType, targetRegion,
                pageNo, active, modifiedDate, syncId);
    }

    private TourApiPage fetch(int contentTypeId, String classification1,
                              String classification2, TourismCategory category,
                              TourismPlaceType placeType, TourismTargetRegion targetRegion,
                              int pageNo, boolean active, String modifiedDate, Long syncId) {
        if (!isConfigured()) {
            throw new BusinessException(TourismErrorCode.API_KEY_NOT_CONFIGURED);
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/areaBasedSyncList2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("numOfRows", pageSize)
                .queryParam("pageNo", pageNo)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("arrange", "C")
                .queryParam("showflag", active ? "1" : "0")
                .queryParam("contentTypeId", contentTypeId)
                .queryParam("lDongRegnCd", targetRegion.getLegalDongRegionCode());
        if (StringUtils.hasText(classification1)) {
            builder.queryParam("lclsSystm1", classification1);
        }
        if (StringUtils.hasText(classification2)) {
            builder.queryParam("lclsSystm2", classification2);
        }
        if (StringUtils.hasText(targetRegion.getLegalDongSigunguCode())) {
            builder.queryParam("lDongSignguCd", targetRegion.getLegalDongSigunguCode());
        }
        if (StringUtils.hasText(modifiedDate)) {
            builder.queryParam("modifiedtime", modifiedDate);
        }

        URI uri = builder.build().encode().toUri();
        try {
            String json = restTemplate.getForObject(uri, String.class);
            return parse(json, category, placeType, contentTypeId, active, syncId);
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException(TourismErrorCode.TOUR_API_ERROR, e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(TourismErrorCode.TOUR_API_ERROR,
                    "TourAPI 응답을 읽을 수 없습니다: " + e.getMessage());
        }
    }

    public TourApiDetail fetchDetail(String contentId, int contentTypeId) {
        JsonNode common = fetchDetailItem("detailCommon2", contentId, null);
        JsonNode intro = fetchDetailItem("detailIntro2", contentId, contentTypeId);

        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("소개", text(common, "overview"));
        sections.put("홈페이지", text(common, "homepage"));
        sections.put("이용기간", firstText(intro,
                "openperiod", "opendate", "opendatefood"));
        sections.put("이용시간", firstText(intro,
                "usetimeleports", "usetime", "opentimefood"));
        sections.put("휴무일", firstText(intro,
                "restdateleports", "restdate", "restdatefood"));
        sections.put("이용요금", firstText(intro,
                "usefeeleports", "usefee"));
        sections.put("주차정보", joinDetails(
                firstText(intro, "parkingleports", "parking", "parkingfood",
                        "parkinglodging"),
                text(intro, "parkingfeeleports")));
        sections.put("예약안내", firstText(intro,
                "reservation", "reservationinfo", "reservationfood",
                "reservationlodging"));
        sections.put("체험 가능 연령", firstText(intro,
                "expagerangeleports", "expagerange"));
        sections.put("규모", firstText(intro,
                "scaleleports", "scale", "scalefood", "scalelodging"));
        sections.put("대표메뉴", text(intro, "firstmenu"));
        sections.put("취급메뉴", text(intro, "treatmenu"));
        sections.put("포장", text(intro, "packing"));
        sections.put("좌석", text(intro, "seat"));
        sections.put("유아시설", text(intro, "kidsfacility"));
        sections.put("신용카드", firstText(intro,
                "chkcreditcardleports", "chkcreditcard", "chkcreditcardfood"));
        sections.put("유모차", firstText(intro,
                "chkbabycarriageleports", "chkbabycarriage"));
        sections.put("반려동물", firstText(intro,
                "chkpetleports", "chkpet"));
        sections.put("입실시간", text(intro, "checkintime"));
        sections.put("퇴실시간", text(intro, "checkouttime"));
        sections.put("객실유형", text(intro, "roomtype"));
        sections.put("객실수", text(intro, "roomcount"));
        sections.put("취사여부", text(intro, "chkcooking"));
        sections.put("식음료장", text(intro, "foodplace"));
        sections.put("부대시설", text(intro, "subfacility"));
        sections.put("픽업서비스", text(intro, "pickup"));

        StringBuilder description = new StringBuilder();
        for (Map.Entry<String, String> section : sections.entrySet()) {
            String value = cleanHtml(section.getValue());
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if (description.length() > 0) {
                description.append("\n\n");
            }
            description.append('[').append(section.getKey()).append("]\n")
                    .append(value);
        }
        String phoneNumber = extractPhoneNumber(firstText(common, "tel"));
        if (!StringUtils.hasText(phoneNumber)) {
            phoneNumber = extractPhoneNumber(firstText(intro,
                    "infocenterleports", "infocenter", "infocenterfood",
                    "infocenterlodging", "reservationlodging"));
        }
        return new TourApiDetail(
                description.length() == 0 ? null : description.toString(),
                phoneNumber,
                normalizeTime(text(intro, "checkintime")),
                normalizeTime(text(intro, "checkouttime")));
    }

    private JsonNode fetchDetailItem(String operation, String contentId,
                                     Integer contentTypeId) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/" + operation)
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", mobileApp)
                .queryParam("_type", "json")
                .queryParam("contentId", contentId);
        if (contentTypeId != null) {
            builder.queryParam("contentTypeId", contentTypeId);
        }

        try {
            String json = restTemplate.getForObject(builder.build().encode().toUri(), String.class);
            JsonNode root = objectMapper.readTree(json);
            JsonNode response = root.path("response");
            JsonNode header = response.path("header");
            String resultCode = header.path("resultCode").asText();
            if (!StringUtils.hasText(resultCode)) {
                resultCode = root.path("resultCode").asText();
            }
            if (!("0000".equals(resultCode) || "00".equals(resultCode))) {
                String resultMessage = StringUtils.hasText(header.path("resultMsg").asText())
                        ? header.path("resultMsg").asText()
                        : root.path("resultMsg").asText();
                throw new BusinessException(TourismErrorCode.TOUR_API_ERROR,
                        operation + " 오류(" + resultCode + "): " + resultMessage);
            }
            JsonNode item = response.path("body").path("items").path("item");
            if (item.isArray()) {
                return item.isEmpty() ? objectMapper.createObjectNode() : item.get(0);
            }
            return item.isObject() ? item : objectMapper.createObjectNode();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException(TourismErrorCode.TOUR_API_ERROR, e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(TourismErrorCode.TOUR_API_ERROR,
                    operation + " 응답을 읽을 수 없습니다: " + e.getMessage());
        }
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String joinDetails(String first, String second) {
        if (!StringUtils.hasText(first)) return second;
        if (!StringUtils.hasText(second)) return first;
        return first + " / " + second;
    }

    private String cleanHtml(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String withLines = value.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p\\s*>", "\n")
                .replaceAll("<[^>]+>", "");
        return HtmlUtils.htmlUnescape(withLines)
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String extractPhoneNumber(String value) {
        String cleaned = cleanHtml(value);
        if (!StringUtils.hasText(cleaned)) {
            return null;
        }
        Matcher matcher = PHONE_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            return matcher.group().replaceAll("[^0-9]+", "-");
        }
        String compact = cleaned.replaceAll("[^0-9]", "");
        return compact.length() >= 3 && compact.length() <= 4 ? compact : null;
    }

    private String normalizeTime(String value) {
        String cleaned = cleanHtml(value);
        if (!StringUtils.hasText(cleaned)) return null;
        Matcher matcher = Pattern.compile("(?<!\\d)([01]?\\d|2[0-3])[:시\\s]+([0-5]?\\d)(?!\\d)")
                .matcher(cleaned);
        if (!matcher.find()) return null;
        return String.format("%02d:%02d:00",
                Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    private TourApiPage parse(String json, TourismCategory category,
                              TourismPlaceType placeType, int contentTypeId,
                              boolean active, Long syncId)
            throws Exception {
        JsonNode response = objectMapper.readTree(json).path("response");
        JsonNode header = response.path("header");
        String resultCode = header.path("resultCode").asText();
        if (!("0000".equals(resultCode) || "00".equals(resultCode))) {
            throw new BusinessException(TourismErrorCode.TOUR_API_ERROR,
                    "TourAPI 오류(" + resultCode + "): " + header.path("resultMsg").asText());
        }

        JsonNode body = response.path("body");
        JsonNode itemNode = body.path("items").path("item");
        List<TourApiItem> items = parseItems(itemNode, category, placeType,
                contentTypeId, active, syncId);
        return new TourApiPage(items, body.path("numOfRows").asInt(pageSize),
                body.path("totalCount").asInt(items.size()));
    }

    private List<TourApiItem> parseItems(JsonNode node, TourismCategory category,
                                         TourismPlaceType placeType, int contentTypeId,
                                         boolean active, Long syncId) {
        if (node.isMissingNode() || node.isNull() || node.isTextual()) {
            return Collections.emptyList();
        }
        List<TourApiItem> items = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                add(items, item, category, placeType, contentTypeId, active, syncId);
            }
        } else if (node.isObject()) {
            add(items, node, category, placeType, contentTypeId, active, syncId);
        }
        return items;
    }

    private void add(List<TourApiItem> items, JsonNode node, TourismCategory category,
                     TourismPlaceType placeType, int contentTypeId,
                     boolean active, Long syncId) {
        String contentId = text(node, "contentid");
        String title = text(node, "title");
        if (!StringUtils.hasText(contentId) || !StringUtils.hasText(title)) {
            return;
        }
        TourApiItem item = new TourApiItem();
        item.setContentId(contentId);
        item.setTitle(title);
        item.setAddress1(text(node, "addr1"));
        item.setAddress2(text(node, "addr2"));
        item.setLongitude(decimal(node, "mapx"));
        item.setLatitude(decimal(node, "mapy"));
        item.setPhoneNumber(extractPhoneNumber(text(node, "tel")));
        item.setThumbnailUrl(text(node, "firstimage2"));
        item.setModifiedTime(text(node, "modifiedtime"));
        item.setClassification1(firstText(node, "lclsSystm1", "lclssystm1"));
        item.setClassification2(firstText(node, "lclsSystm2", "lclssystm2"));
        item.setClassification3(firstText(node, "lclsSystm3", "lclssystm3"));
        item.setActive(active);
        item.setCategory(category);
        item.setPlaceType(placeType);
        item.setContentTypeId(contentTypeId);
        item.setSyncId(syncId);
        items.add(item);
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        try {
            return value == null ? null : new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeServiceKey(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.matches(".*%[0-9A-Fa-f]{2}.*")) {
            try {
                return URLDecoder.decode(trimmed, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException exception) {
                // UTF-8은 모든 Java 구현이 반드시 지원하므로 실행될 수 없는 방어 코드다.
                throw new IllegalStateException("UTF-8 디코딩을 지원하지 않습니다.", exception);
            }
        }
        return trimmed;
    }
}
