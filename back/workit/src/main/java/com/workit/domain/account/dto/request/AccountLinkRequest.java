package com.workit.domain.account.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AccountLinkRequest {
    private List<Long> linkableAccountIds;
}