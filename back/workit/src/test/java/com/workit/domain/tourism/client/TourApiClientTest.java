package com.workit.domain.tourism.client;

import com.workit.domain.tourism.vo.TourApiPage;
import com.workit.domain.tourism.vo.TourApiDetail;
import com.workit.domain.tourism.vo.TourismCategory;
import com.workit.domain.tourism.vo.TourismTargetRegion;
import com.workit.domain.tourism.vo.TourismPlaceType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TourApiClientTest {
    @Test
    void callsTourApiWithInjectedRestTemplateAndParsesResponse() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(response());
        TourApiClient client = new TourApiClient(restTemplate,
                "https://example.test/KorService2", "decode-key", "Workit", 100);

        TourApiPage page = client.fetch(TourismCategory.WATER_SPORTS,
                TourismTargetRegion.GANGNEUNG,
                1, true, "20260818", 9L);

        assertEquals(1, page.getItems().size());
        assertEquals("100", page.getItems().get(0).getContentId());
        assertEquals(TourismCategory.WATER_SPORTS, page.getItems().get(0).getCategory());
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForObject(uri.capture(), eq(String.class));
        assertTrue(uri.getValue().toString().contains("lclsSystm2=LS02"));
        assertTrue(uri.getValue().toString().contains("lDongRegnCd=51"));
        assertTrue(uri.getValue().toString().contains("lDongSignguCd=150"));
        assertTrue(uri.getValue().toString().contains("modifiedtime=20260818"));
    }

    @Test
    void decodesPortalEncodingKeyBeforeBuildingRequestUri() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(response());
        TourApiClient client = new TourApiClient(restTemplate,
                "https://example.test/KorService2",
                "sample%2Fkey%2Bvalue%3D", "Workit", 100);

        client.fetch(TourismCategory.WATER_SPORTS, TourismTargetRegion.BUSAN,
                1, true, null, 9L);

        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForObject(uri.capture(), eq(String.class));
        assertTrue(uri.getValue().getQuery().contains("serviceKey=sample/key+value="));
        assertTrue(!uri.getValue().getRawQuery().contains("%252F"));
    }

    @Test
    void combinesCommonAndIntroDetailsIntoPlainTextDescription() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(commonDetailResponse(), introDetailResponse());
        TourApiClient client = new TourApiClient(restTemplate,
                "https://example.test/KorService2", "decode-key", "Workit", 100);

        TourApiDetail detail = client.fetchDetail("100", 28);
        String description = detail.getDescription();

        assertTrue(description.contains("[소개]\n바다에서 즐기는\n수상 레포츠입니다."));
        assertTrue(description.contains("[이용시간]\n09:00~18:00"));
        assertTrue(description.contains("[주차정보]\n주차 가능 / 무료"));
        assertTrue(description.contains("[예약안내]\n전화 예약"));
        assertTrue(!description.contains("<br>"));
        assertTrue(!description.contains("[문의]"));
        assertEquals("051-123-4567", detail.getPhoneNumber());
    }

    @Test
    void extractsFourDigitSafePhonePrefix() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(commonDetailWithSafePhoneResponse(), introDetailResponse());
        TourApiClient client = new TourApiClient(restTemplate,
                "https://example.test/KorService2", "decode-key", "Workit", 100);

        TourApiDetail detail = client.fetchDetail("100", 39);

        assertEquals("0507-1358-7490", detail.getPhoneNumber());
    }

    @Test
    void extractsNationalServicePhoneNumber() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(commonDetailWithServicePhoneResponse(), introDetailResponse());
        TourApiClient client = new TourApiClient(restTemplate,
                "https://example.test/KorService2", "decode-key", "Workit", 100);

        TourApiDetail detail = client.fetchDetail("100", 39);

        assertEquals("1833-4212", detail.getPhoneNumber());
    }

    @Test
    void requestsAllJejuRestaurantsWithoutNarrowClassificationFilter() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(response());
        TourApiClient client = new TourApiClient(restTemplate,
                "https://example.test/KorService2", "decode-key", "Workit", 100);

        client.fetch(TourismPlaceType.RESTAURANT, TourismTargetRegion.JEJU,
                1, true, null, 9L);

        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForObject(uri.capture(), eq(String.class));
        assertTrue(uri.getValue().toString().contains("contentTypeId=39"));
        assertTrue(uri.getValue().toString().contains("lDongRegnCd=50"));
        assertTrue(!uri.getValue().toString().contains("lclsSystm2="));
    }

    @Test
    void extractsAccommodationPhoneAndCheckTimesFromIntroDetail() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn(emptyCommonDetailResponse(), lodgingIntroDetailResponse());
        TourApiClient client = new TourApiClient(restTemplate,
                "https://example.test/KorService2", "decode-key", "Workit", 100);

        TourApiDetail detail = client.fetchDetail("200", 32);

        assertEquals("064-123-4567", detail.getPhoneNumber());
        assertEquals("15:00:00", detail.getCheckInTime());
        assertEquals("11:00:00", detail.getCheckOutTime());
        assertTrue(detail.getDescription().contains("[입실시간]\n15:00 이후"));
        assertTrue(detail.getDescription().contains("[부대시설]\n수영장, 세미나실"));
    }

    private String response() {
        return "{\"response\":{\"header\":{\"resultCode\":\"0000\",\"resultMsg\":\"OK\"},"
                + "\"body\":{\"items\":{\"item\":[{\"contentid\":\"100\","
                + "\"title\":\"광안리 수상레저\",\"addr1\":\"부산광역시 수영구\","
                + "\"mapx\":\"129.1\",\"mapy\":\"35.1\",\"modifiedtime\":\"20260818120000\"}]},"
                + "\"numOfRows\":100,\"totalCount\":1}}}";
    }

    private String commonDetailResponse() {
        return "{\"response\":{\"header\":{\"resultCode\":\"0000\","
                + "\"resultMsg\":\"OK\"},\"body\":{\"items\":{\"item\":[{"
                + "\"contentid\":\"100\",\"tel\":\"문의 051) 123-4567\",\"overview\":"
                + "\"바다에서 즐기는<br>수상 레포츠입니다.\"}]}}}}";
    }

    private String introDetailResponse() {
        return "{\"response\":{\"header\":{\"resultCode\":\"0000\","
                + "\"resultMsg\":\"OK\"},\"body\":{\"items\":{\"item\":[{"
                + "\"contentid\":\"100\",\"contenttypeid\":\"28\","
                + "\"usetimeleports\":\"09:00~18:00\","
                + "\"parkingleports\":\"주차 가능\","
                + "\"parkingfeeleports\":\"무료\","
                + "\"reservation\":\"전화 예약\"}]}}}}";
    }

    private String commonDetailWithSafePhoneResponse() {
        return "{\"response\":{\"header\":{\"resultCode\":\"0000\","
                + "\"resultMsg\":\"OK\"},\"body\":{\"items\":{\"item\":[{"
                + "\"contentid\":\"100\",\"tel\":\"0507-1358-7490\","
                + "\"overview\":\"카페 소개\"}]}}}}";
    }

    private String commonDetailWithServicePhoneResponse() {
        return "{\"response\":{\"header\":{\"resultCode\":\"0000\","
                + "\"resultMsg\":\"OK\"},\"body\":{\"items\":{\"item\":[{"
                + "\"contentid\":\"100\",\"tel\":\"대표번호 1833-4212\","
                + "\"overview\":\"시설 소개\"}]}}}}";
    }

    private String emptyCommonDetailResponse() {
        return "{\"response\":{\"header\":{\"resultCode\":\"0000\","
                + "\"resultMsg\":\"OK\"},\"body\":{\"items\":{\"item\":[{"
                + "\"contentid\":\"200\"}]}}}}";
    }

    private String lodgingIntroDetailResponse() {
        return "{\"response\":{\"header\":{\"resultCode\":\"0000\","
                + "\"resultMsg\":\"OK\"},\"body\":{\"items\":{\"item\":[{"
                + "\"contentid\":\"200\",\"contenttypeid\":\"32\","
                + "\"infocenterlodging\":\"예약 문의 064-123-4567\","
                + "\"checkintime\":\"15:00 이후\",\"checkouttime\":\"11:00 이전\","
                + "\"subfacility\":\"수영장, 세미나실\"}]}}}}";
    }
}
