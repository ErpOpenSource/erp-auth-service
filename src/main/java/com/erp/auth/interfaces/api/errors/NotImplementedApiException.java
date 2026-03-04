package com.erp.auth.interfaces.api.errors;

import org.springframework.http.HttpStatus;

public class NotImplementedApiException extends ApiException {
    public NotImplementedApiException(String feature) {
        super(
                ErrorCode.INTERNAL_ERROR,
                HttpStatus.NOT_IMPLEMENTED,
                "Not implemented yet: " + feature
        );
    }
}