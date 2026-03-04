package com.erp.auth.application.admin;

import com.erp.auth.application.audit.AuditService;
import com.erp.auth.infrastructure.persistence.jpa.entity.DepartmentEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.ModuleEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.RoleEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserDepartmentEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserModuleEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserRoleEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.DepartmentJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.ModuleJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.RoleJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserDepartmentJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserModuleJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserRoleJpaRepository;
import com.erp.auth.infrastructure.security.PasswordHasher;
import com.erp.auth.interfaces.api.dto.AdminCreateUserRequest;
import com.erp.auth.interfaces.api.dto.AdminUserResponse;
import com.erp.auth.interfaces.api.errors.ApiException;
import com.erp.auth.interfaces.api.errors.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminUsersUseCase {

    private final UserJpaRepository userRepo;
    private final RoleJpaRepository roleRepo;
    private final ModuleJpaRepository moduleRepo;
    private final DepartmentJpaRepository departmentRepo;
    private final UserRoleJpaRepository userRoleRepo;
    private final UserModuleJpaRepository userModuleRepo;
    private final UserDepartmentJpaRepository userDepartmentRepo;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;

    public AdminUsersUseCase(
            UserJpaRepository userRepo,
            RoleJpaRepository roleRepo,
            ModuleJpaRepository moduleRepo,
            DepartmentJpaRepository departmentRepo,
            UserRoleJpaRepository userRoleRepo,
            UserModuleJpaRepository userModuleRepo,
            UserDepartmentJpaRepository userDepartmentRepo,
            PasswordHasher passwordHasher,
            AuditService auditService
    ) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.moduleRepo = moduleRepo;
        this.departmentRepo = departmentRepo;
        this.userRoleRepo = userRoleRepo;
        this.userModuleRepo = userModuleRepo;
        this.userDepartmentRepo = userDepartmentRepo;
        this.passwordHasher = passwordHasher;
        this.auditService = auditService;
    }

    @Transactional
    public AdminUserResponse createUser(
            UUID actorUserId,
            AdminCreateUserRequest request,
            String ip,
            String userAgent
    ) {
        String username = request.username().trim();
        String email = request.email() == null ? null : request.email().trim();
        if (email != null && email.isBlank()) {
            email = null;
        }

        if (userRepo.findByUsername(username).isPresent()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.CONFLICT,
                    "Username already exists.",
                    Map.of("field", "username")
            );
        }
        if (email != null && userRepo.findByEmail(email).isPresent()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.CONFLICT,
                    "Email already exists.",
                    Map.of("field", "email")
            );
        }

        List<String> roleCodes = normalizeRoleCodes(request.roles());
        List<String> moduleCodes = normalizeCodes(request.modules());
        List<String> departmentCodes = normalizeCodes(request.departments());

        List<RoleEntity> roles = roleRepo.findByCodeIn(roleCodes);
        if (roles.size() != roleCodes.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Unknown role code(s).",
                    Map.of("roles", findMissing(roleCodes, roles.stream().map(RoleEntity::getCode).toList()))
            );
        }

        List<ModuleEntity> modules = moduleRepo.findByCodeIn(moduleCodes);
        if (modules.size() != moduleCodes.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Unknown module code(s).",
                    Map.of("modules", findMissing(moduleCodes, modules.stream().map(ModuleEntity::getCode).toList()))
            );
        }

        List<DepartmentEntity> departments = departmentRepo.findByCodeIn(departmentCodes);
        if (departments.size() != departmentCodes.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Unknown department code(s).",
                    Map.of("departments", findMissing(departmentCodes, departments.stream().map(DepartmentEntity::getCode).toList()))
            );
        }

        OffsetDateTime now = OffsetDateTime.now();
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHasher.hash(request.password()));
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user = userRepo.save(user);

        for (RoleEntity role : roles) {
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRole.setAssignedAt(now);
            userRoleRepo.save(userRole);
        }
        for (ModuleEntity module : modules) {
            UserModuleEntity userModule = new UserModuleEntity();
            userModule.setUserId(user.getId());
            userModule.setModuleId(module.getId());
            userModule.setAssignedAt(now);
            userModuleRepo.save(userModule);
        }
        for (DepartmentEntity department : departments) {
            UserDepartmentEntity userDepartment = new UserDepartmentEntity();
            userDepartment.setUserId(user.getId());
            userDepartment.setDepartmentId(department.getId());
            userDepartment.setAssignedAt(now);
            userDepartmentRepo.save(userDepartment);
        }

        UserEntity actor = userRepo.findById(actorUserId).orElse(null);
        auditService.record(
                "ADMIN_USER_CREATE",
                actor,
                user,
                null,
                "{\"username\":\"" + safe(username) + "\",\"roles\":[" + toJsonArray(roleCodes) + "],"
                        + "\"modules\":[" + toJsonArray(moduleCodes) + "],"
                        + "\"departments\":[" + toJsonArray(departmentCodes) + "],"
                        + "\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}"
        );

        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus().name(),
                roleCodes,
                moduleCodes,
                departmentCodes
        );
    }

    @Transactional
    public void lockUser(UUID actorUserId, UUID targetUserId, String ip, String userAgent) {
        updateStatus(actorUserId, targetUserId, UserEntity.UserStatus.LOCKED, "ADMIN_USER_LOCK", ip, userAgent);
    }

    @Transactional
    public void disableUser(UUID actorUserId, UUID targetUserId, String ip, String userAgent) {
        updateStatus(actorUserId, targetUserId, UserEntity.UserStatus.DISABLED, "ADMIN_USER_DISABLE", ip, userAgent);
    }

    @Transactional
    public void enableUser(UUID actorUserId, UUID targetUserId, String ip, String userAgent) {
        updateStatus(actorUserId, targetUserId, UserEntity.UserStatus.ACTIVE, "ADMIN_USER_ENABLE", ip, userAgent);
    }

    @Transactional
    public void resetPassword(UUID actorUserId, UUID targetUserId, String newPassword, String ip, String userAgent) {
        UserEntity actor = userRepo.findById(actorUserId).orElse(null);
        UserEntity target = userRepo.findById(targetUserId).orElse(null);

        if (target == null) {
            auditService.record(
                    "ADMIN_USER_PASSWORD_RESET",
                    actor,
                    null,
                    null,
                    "{\"targetUserId\":\"" + targetUserId + "\",\"result\":\"NOT_FOUND\","
                            + "\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}"
            );
            return;
        }

        target.setPasswordHash(passwordHasher.hash(newPassword));
        target.setUpdatedAt(OffsetDateTime.now());
        userRepo.save(target);

        auditService.record(
                "ADMIN_USER_PASSWORD_RESET",
                actor,
                target,
                null,
                "{\"targetUserId\":\"" + targetUserId + "\",\"result\":\"UPDATED\","
                        + "\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}"
        );
    }

    private void updateStatus(
            UUID actorUserId,
            UUID targetUserId,
            UserEntity.UserStatus newStatus,
            String auditType,
            String ip,
            String userAgent
    ) {
        UserEntity actor = userRepo.findById(actorUserId).orElse(null);
        UserEntity target = userRepo.findById(targetUserId).orElse(null);

        if (target == null) {
            auditService.record(
                    auditType,
                    actor,
                    null,
                    null,
                    "{\"targetUserId\":\"" + targetUserId + "\",\"result\":\"NOT_FOUND\","
                            + "\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}"
            );
            return;
        }

        String result;
        if (target.getStatus() == newStatus) {
            result = "ALREADY_" + newStatus.name();
        } else {
            target.setStatus(newStatus);
            target.setUpdatedAt(OffsetDateTime.now());
            userRepo.save(target);
            result = "UPDATED";
        }

        auditService.record(
                auditType,
                actor,
                target,
                null,
                "{\"targetUserId\":\"" + targetUserId + "\",\"newStatus\":\"" + newStatus.name() + "\","
                        + "\"result\":\"" + result + "\",\"ip\":\"" + safe(ip) + "\","
                        + "\"userAgent\":\"" + safe(userAgent) + "\"}"
        );
    }

    private static List<String> normalizeRoleCodes(List<String> rawRoles) {
        if (rawRoles == null || rawRoles.isEmpty()) {
            return List.of("USER");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String role : rawRoles) {
            if (role == null) {
                continue;
            }
            String cleaned = role.trim().toUpperCase();
            if (!cleaned.isEmpty()) {
                normalized.add(cleaned);
            }
        }
        if (normalized.isEmpty()) {
            return List.of("USER");
        }
        return List.copyOf(normalized);
    }

    private static String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(safe(values.get(i))).append('"');
        }
        return sb.toString();
    }

    private static List<String> findMissing(List<String> expected, List<String> found) {
        Set<String> foundSet = new LinkedHashSet<>(found);
        List<String> missing = new ArrayList<>();
        for (String code : expected) {
            if (!foundSet.contains(code)) {
                missing.add(code);
            }
        }
        return missing;
    }

    private static List<String> normalizeCodes(List<String> rawCodes) {
        if (rawCodes == null || rawCodes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String code : rawCodes) {
            if (code == null) {
                continue;
            }
            String cleaned = code.trim().toUpperCase();
            if (!cleaned.isEmpty()) {
                normalized.add(cleaned);
            }
        }
        return List.copyOf(normalized);
    }

    private static String safe(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\"", "'");
    }
}
