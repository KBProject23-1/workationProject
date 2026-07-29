package com.workit.domain.account.dto.response;

import lombok.Data;

@Data
public class PrimaryAccountResponse {
    private Long accountId;
    private Boolean isPrimary;
}