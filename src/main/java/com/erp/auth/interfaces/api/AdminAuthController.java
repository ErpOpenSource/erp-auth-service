package com.erp.auth.interfaces.api;

import com.erp.auth.application.admin.AdminAuthorizationUseCase;
import com.erp.auth.application.admin.AdminLicensesUseCase;
import com.erp.auth.application.admin.AdminSessionsUseCase;
import com.erp.auth.application.admin.AdminUsersUseCase;
import com.erp.auth.interfaces.api.dto.ActiveSessionsResponse;
import com.erp.auth.interfaces.api.dto.AdminCodeNameResponse;
import com.erp.auth.interfaces.api.dto.AdminCreateDepartmentRequest;
import com.erp.auth.interfaces.api.dto.AdminCreateModuleRequest;
import com.erp.auth.interfaces.api.dto.AdminCreatePermissionRequest;
import com.erp.auth.interfaces.api.dto.AdminCreateUserRequest;
import com.erp.auth.interfaces.api.dto.AdminResetPasswordRequest;
import com.erp.auth.interfaces.api.dto.AdminUserAccessResponse;
import com.erp.auth.interfaces.api.dto.AdminUserResponse;
import com.erp.auth.interfaces.api.dto.CodeAssignmentRequest;
import com.erp.auth.interfaces.api.dto.SeatsResponse;
import com.erp.auth.interfaces.api.dto.UpdateSeatsRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    private final AdminAuthorizationUseCase adminAuthorizationUseCase;
    private final AdminLicensesUseCase adminLicensesUseCase;
    private final AdminSessionsUseCase adminSessionsUseCase;
    private final AdminUsersUseCase adminUsersUseCase;

    public AdminAuthController(
            AdminAuthorizationUseCase adminAuthorizationUseCase,
            AdminLicensesUseCase adminLicensesUseCase,
            AdminSessionsUseCase adminSessionsUseCase,
            AdminUsersUseCase adminUsersUseCase
    ) {
        this.adminAuthorizationUseCase = adminAuthorizationUseCase;
        this.adminLicensesUseCase = adminLicensesUseCase;
        this.adminSessionsUseCase = adminSessionsUseCase;
        this.adminUsersUseCase = adminUsersUseCase;
    }

    @GetMapping("/licenses/seats")
    public SeatsResponse getSeats(@AuthenticationPrincipal Jwt jwt, HttpServletRequest http) {
        return adminLicensesUseCase.getSeats(actorUserId(jwt), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PutMapping("/licenses/seats")
    public SeatsResponse updateSeats(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateSeatsRequest request,
            HttpServletRequest http
    ) {
        return adminLicensesUseCase.updateSeats(
                actorUserId(jwt),
                request,
                http.getRemoteAddr(),
                http.getHeader("User-Agent")
        );
    }

    @GetMapping("/sessions/active")
    public ActiveSessionsResponse getActiveSessions(@AuthenticationPrincipal Jwt jwt, HttpServletRequest http) {
        return adminSessionsUseCase.listActive(actorUserId(jwt), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/sessions/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID sessionId,
            HttpServletRequest http
    ) {
        adminSessionsUseCase.revoke(actorUserId(jwt), sessionId, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse createUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminCreateUserRequest request,
            HttpServletRequest http
    ) {
        return adminUsersUseCase.createUser(actorUserId(jwt), request, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/users/{id}/lock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lockUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID userId,
            HttpServletRequest http
    ) {
        adminUsersUseCase.lockUser(actorUserId(jwt), userId, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/users/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID userId,
            HttpServletRequest http
    ) {
        adminUsersUseCase.disableUser(actorUserId(jwt), userId, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/users/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enableUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID userId,
            HttpServletRequest http
    ) {
        adminUsersUseCase.enableUser(actorUserId(jwt), userId, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/users/{id}/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID userId,
            @Valid @RequestBody AdminResetPasswordRequest request,
            HttpServletRequest http
    ) {
        adminUsersUseCase.resetPassword(
                actorUserId(jwt),
                userId,
                request.newPassword(),
                http.getRemoteAddr(),
                http.getHeader("User-Agent")
        );
    }

    @PostMapping("/modules")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCodeNameResponse createModule(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminCreateModuleRequest request,
            HttpServletRequest http
    ) {
        return adminAuthorizationUseCase.createModule(actorUserId(jwt), request, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCodeNameResponse createDepartment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminCreateDepartmentRequest request,
            HttpServletRequest http
    ) {
        return adminAuthorizationUseCase.createDepartment(actorUserId(jwt), request, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PostMapping("/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCodeNameResponse createPermission(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminCreatePermissionRequest request,
            HttpServletRequest http
    ) {
        return adminAuthorizationUseCase.createPermission(actorUserId(jwt), request, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PutMapping("/users/{id}/modules")
    public CodeAssignmentRequest assignUserModules(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID userId,
            @Valid @RequestBody CodeAssignmentRequest request,
            HttpServletRequest http
    ) {
        return new CodeAssignmentRequest(
                adminAuthorizationUseCase.assignUserModules(actorUserId(jwt), userId, request.codes(), http.getRemoteAddr(), http.getHeader("User-Agent"))
        );
    }

    @PutMapping("/users/{id}/departments")
    public CodeAssignmentRequest assignUserDepartments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID userId,
            @Valid @RequestBody CodeAssignmentRequest request,
            HttpServletRequest http
    ) {
        return new CodeAssignmentRequest(
                adminAuthorizationUseCase.assignUserDepartments(actorUserId(jwt), userId, request.codes(), http.getRemoteAddr(), http.getHeader("User-Agent"))
        );
    }

    @PutMapping("/roles/{code}/permissions")
    public CodeAssignmentRequest assignRolePermissions(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("code") String roleCode,
            @Valid @RequestBody CodeAssignmentRequest request,
            HttpServletRequest http
    ) {
        return new CodeAssignmentRequest(
                adminAuthorizationUseCase.assignRolePermissions(actorUserId(jwt), roleCode, request.codes(), http.getRemoteAddr(), http.getHeader("User-Agent"))
        );
    }

    @GetMapping("/users/{id}/access")
    public AdminUserAccessResponse getUserAccess(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID userId,
            HttpServletRequest http
    ) {
        return adminAuthorizationUseCase.getUserAccess(actorUserId(jwt), userId, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    private static UUID actorUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
