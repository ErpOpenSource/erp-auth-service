package com.erp.auth.interfaces.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CodeAssignmentRequest(
        @NotNull List<String> codes
) {}
