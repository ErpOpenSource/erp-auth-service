package com.erp.auth.application.auth;

import java.util.List;

public record UserAuthorizationContext(
        List<String> roles,
        List<String> modules,
        List<String> departments,
        List<String> permissions
) {}
