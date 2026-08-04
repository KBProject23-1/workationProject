package com.workit.domain.merchant.offices.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MerchantOfficeListResponseDTO {

    private final List<MerchantOfficeItemResponseDTO> content;
    private final String nextCursor;
    private final int size;
    private final boolean hasNext;

    public static MerchantOfficeListResponseDTO of(
            List<MerchantOfficeItemResponseDTO> content,
            String nextCursor,
            int size,
            boolean hasNext
    ) {
        return MerchantOfficeListResponseDTO.builder()
                .content(content)
                .nextCursor(nextCursor)
                .size(size)
                .hasNext(hasNext)
                .build();
    }
}
