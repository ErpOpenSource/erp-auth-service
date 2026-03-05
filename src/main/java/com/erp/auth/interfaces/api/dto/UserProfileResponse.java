package com.erp.auth.interfaces.api.dto;

import java.util.List;
<<<<<<< HEAD
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
=======

public record UserProfileResponse(
        String id,
>>>>>>> dd5ee767521ad1cf359493a0c563a84ff7327432
        String username,
        String email,
        String status,
        List<String> roles,
        List<String> modules,
<<<<<<< HEAD
        List<String> departments,
=======
>>>>>>> dd5ee767521ad1cf359493a0c563a84ff7327432
        List<String> permissions
) {}
