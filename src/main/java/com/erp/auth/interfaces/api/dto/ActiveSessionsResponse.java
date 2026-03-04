package com.erp.auth.interfaces.api.dto;

import java.util.List;

public record ActiveSessionsResponse(
        List<ActiveSessionItem> items
) {}