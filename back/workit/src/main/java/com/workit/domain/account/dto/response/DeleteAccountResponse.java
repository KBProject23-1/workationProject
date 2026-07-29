package com.workit.domain.account.dto.response;

import lombok.Data;

@Data
public class DeleteAccountResponse {
    private Long accountId;
    private Boolean isDeleted;
}