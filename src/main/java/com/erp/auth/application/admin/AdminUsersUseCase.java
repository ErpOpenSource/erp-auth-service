package com.erp.auth.application.admin;

import com.erp.auth.application.audit.AuditService;
import com.erp.auth.application.auth.UserAuthorizationContext;
import com.erp.auth.application.auth.UserAuthorizationService;
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
import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserDepartmentJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserModuleJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserRoleJpaRepository;
import com.erp.auth.infrastructure.security.PasswordHasher;
import com.erp.auth.interfaces.api.dto.AdminCreateUserRequest;
import com.erp.auth.interfaces.api.dto.AdminUpdateUserRequest;
import com.erp.auth.interfaces.api.dto.AdminUserListItem;
import com.erp.auth.interfaces.api.dto.AdminUserResponse;
import org.springframework.data.domain.Sort;
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
import java.util.Objects;
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
    private final SessionJpaRepository sessionRepo;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;
    private final UserAuthorizationService userAuthorizationService;

    public AdminUsersUseCase(
            UserJpaRepository userRepo,
            RoleJpaRepository roleRepo,
            ModuleJpaRepository moduleRepo,
            DepartmentJpaRepository departmentRepo,
            UserRoleJpaRepository userRoleRepo,
            UserModuleJpaRepository userModuleRepo,
            UserDepartmentJpaRepository userDepartmentRepo,
            SessionJpaRepository sessionRepo,
            PasswordHasher passwordHasher,
            AuditService auditService,
            UserAuthorizationService userAuthorizationService
    ) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.moduleRepo = moduleRepo;
        this.departmentRepo = departmentRepo;
        this.userRoleRepo = userRoleRepo;
        this.userModuleRepo = userModuleRepo;
        this.userDepartmentRepo = userDepartmentRepo;
        this.sessionRepo = sessionRepo;
        this.passwordHasher = passwordHasher;
        this.auditService = auditService;
        this.userAuthorizationService = userAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserListItem> listUsers() {
        return userRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(u -> new AdminUserListItem(u.getId(), u.getUsername(), u.getEmail(), u.getStatus().name(), u.getCreatedAt()))
                .toList();
    }

    @Transactional
    public AdminUserResponse createUser(
            UUID actorUserId,
            AdminCreateUserRequest request,
            String ip,
            String userAgent
    ) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        ensureUniqueUsername(username, null);
        ensureUniqueEmail(email, null);
        ResolvedAssignments assignments = resolveAssignments(request.roles(), request.modules(), request.departments());

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

        saveAssignments(user.getId(), now, assignments);

        UserEntity actor = userRepo.findById(actorUserId).orElse(null);
        auditService.record(
                "ADMIN_USER_CREATE",
                actor,
                user,
                null,
                "{\"username\":\"" + safe(username) + "\",\"roles\":[" + toJsonArray(assignments.roleCodes()) + "],"
                        + "\"modules\":[" + toJsonArray(assignments.moduleCodes()) + "],"
                        + "\"departments\":[" + toJsonArray(assignments.departmentCodes()) + "],"
                        + "\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}"
        );

        UserAuthorizationContext context = userAuthorizationService.resolveForUser(user.getId());
        return toResponse(user, context);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(
            UUID actorUserId,
            UUID targetUserId,
            String ip,
            String userAgent
    ) {
        UserEntity actor = userRepo.findById(actorUserId).orElse(null);
        UserEntity target = requiredUser(targetUserId);
        UserAuthorizationContext context = userAuthorizationService.resolveForUser(targetUserId);

        auditService.record(
                "ADMIN_USER_VIEW",
                actor,
                target,
                null,
                "{\"targetUserId\":\"" + targetUserId + "\",\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}"
        );
        return toResponse(target, context);
    }

    @Transactional
    public AdminUserResponse updateUser(
            UUID actorUserId,
            UUID targetUserId,
            AdminUpdateUserRequest request,
            String ip,
            String userAgent
    ) {
        UserEntity actor = userRepo.findById(actorUserId).orElse(null);
        UserEntity target = requiredUser(targetUserId);

        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        UserEntity.UserStatus newStatus = normalizeStatus(request.status());
        ResolvedAssignments assignments = resolveAssignments(request.roles(), request.modules(), request.departments());
        validateSelfUpdateSafety(actorUserId, targetUserId, newStatus, assignments.roleCodes());

        ensureUniqueUsername(username, targetUserId);
        ensureUniqueEmail(email, targetUserId);

        Set<String> currentRoleSet = toNormalizedSet(userRoleRepo.findRoleCodesByUserId(targetUserId), true);
        Set<String> currentModuleSet = toNormalizedSet(userModuleRepo.findModuleCodesByUserId(targetUserId), true);
        Set<String> currentDepartmentSet = toNormalizedSet(userDepartmentRepo.findDepartmentCodesByUserId(targetUserId), true);

        boolean usernameChanged = !Objects.equals(target.getUsername(), username);
        boolean emailChanged = !Objects.equals(target.getEmail(), email);
        boolean statusChanged = target.getStatus() != newStatus;
        boolean rolesChanged = !currentRoleSet.equals(new LinkedHashSet<>(assignments.roleCodes()));
        boolean modulesChanged = !currentModuleSet.equals(new LinkedHashSet<>(assignments.moduleCodes()));
        boolean departmentsChanged = !currentDepartmentSet.equals(new LinkedHashSet<>(assignments.departmentCodes()));
        boolean assignmentsChanged = rolesChanged || modulesChanged || departmentsChanged;
        boolean changed = usernameChanged || emailChanged || statusChanged || assignmentsChanged;

        OffsetDateTime now = OffsetDateTime.now();
        if (changed) {
            target.setUsername(username);
            target.setEmail(email);
            target.setStatus(newStatus);
            target.setUpdatedAt(now);
            userRepo.save(target);
        }

        if (assignmentsChanged) {
            replaceAssignments(targetUserId, now, assignments);
        }

        int revokedSessions = changed ? sessionRepo.revokeAllByUserId(targetUserId, now) : 0;
        UserAuthorizationContext context = userAuthorizationService.resolveForUser(targetUserId);

        auditService.record(
                "ADMIN_USER_UPDATE",
                actor,
                target,
                null,
                "{\"targetUserId\":\"" + targetUserId + "\","
                        + "\"status\":\"" + newStatus.name() + "\","
                        + "\"changed\":" + changed + ","
                        + "\"revokedSessions\":" + revokedSessions + ","
                        + "\"roles\":[" + toJsonArray(assignments.roleCodes()) + "],"
                        + "\"modules\":[" + toJsonArray(assignments.moduleCodes()) + "],"
                        + "\"departments\":[" + toJsonArray(assignments.departmentCodes()) + "],"
                        + "\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}"
        );

        return toResponse(target, context);
    }

    @Transactional
    public List<String> assignUserRoles(UUID actorUserId, UUID userId, List<String> roleCodes, String ip, String userAgent) {
        UserEntity targetUser = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.VALIDATION_ERROR,
                        HttpStatus.NOT_FOUND,
                        "User not found.",
                        Map.of("userId", userId.toString())
                ));

        List<String> normalized = normalizeCodes(roleCodes == null ? List.of() : roleCodes);
        List<RoleEntity> roles = roleRepo.findByCodeIn(normalized);
        if (roles.size() != normalized.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Unknown role code(s).",
                    Map.of("roles", findMissing(normalized, roles.stream().map(RoleEntity::getCode).toList()))
            );
        }

        userRoleRepo.deleteByUserId(userId);
        OffsetDateTime now = OffsetDateTime.now();
        for (RoleEntity role : roles) {
            UserRoleEntity row = new UserRoleEntity();
            row.setUserId(userId);
            row.setRoleId(role.getId());
            row.setAssignedAt(now);
            userRoleRepo.save(row);
        }

        UserEntity actor = userRepo.findById(actorUserId).orElse(null);
        auditService.record("ADMIN_USER_ROLES_ASSIGN", actor, targetUser, null,
                "{\"userId\":\"" + userId + "\",\"roles\":[" + toJsonArray(normalized) + "],"
                        + "\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}");
        return normalized;
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

        OffsetDateTime now = OffsetDateTime.now();
        target.setPasswordHash(passwordHasher.hash(newPassword));
        target.setUpdatedAt(now);
        userRepo.save(target);
        int revokedSessions = sessionRepo.revokeAllByUserId(targetUserId, now);

        auditService.record(
                "ADMIN_USER_PASSWORD_RESET",
                actor,
                target,
                null,
                "{\"targetUserId\":\"" + targetUserId + "\",\"result\":\"UPDATED\","
                        + "\"revokedSessions\":" + revokedSessions + ","
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

        if (actorUserId.equals(targetUserId) && newStatus != UserEntity.UserStatus.ACTIVE) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "You cannot lock or disable your own account.",
                    Map.of("status", newStatus.name())
            );
        }

        String result;
        int revokedSessions = 0;
        if (target.getStatus() == newStatus) {
            result = "ALREADY_" + newStatus.name();
        } else {
            OffsetDateTime now = OffsetDateTime.now();
            target.setStatus(newStatus);
            target.setUpdatedAt(now);
            userRepo.save(target);
            revokedSessions = sessionRepo.revokeAllByUserId(targetUserId, now);
            result = "UPDATED";
        }

        auditService.record(
                auditType,
                actor,
                target,
                null,
                "{\"targetUserId\":\"" + targetUserId + "\",\"newStatus\":\"" + newStatus.name() + "\","
                        + "\"result\":\"" + result + "\",\"revokedSessions\":" + revokedSessions + ","
                        + "\"ip\":\"" + safe(ip) + "\","
                        + "\"userAgent\":\"" + safe(userAgent) + "\"}"
        );
    }

    private ResolvedAssignments resolveAssignments(
            List<String> rawRoles,
            List<String> rawModules,
            List<String> rawDepartments
    ) {
        List<String> roleCodes = normalizeRoleCodes(rawRoles);
        List<String> moduleCodes = normalizeCodes(rawModules);
        List<String> departmentCodes = normalizeCodes(rawDepartments);

        List<RoleEntity> roles = roleRepo.findByCodeIn(roleCodes);
        if (roles.size() != roleCodes.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Unknown role code(s).",
                    Map.of("roles", findMissing(roleCodes, roles.stream().map(RoleEntity::getCode).toList()))
            );
        }

        List<ModuleEntity> modules = moduleCodes.isEmpty() ? List.of() : moduleRepo.findByCodeIn(moduleCodes);
        if (modules.size() != moduleCodes.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Unknown module code(s).",
                    Map.of("modules", findMissing(moduleCodes, modules.stream().map(ModuleEntity::getCode).toList()))
            );
        }

        List<DepartmentEntity> departments = departmentCodes.isEmpty() ? List.of() : departmentRepo.findByCodeIn(departmentCodes);
        if (departments.size() != departmentCodes.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Unknown department code(s).",
                    Map.of("departments", findMissing(departmentCodes, departments.stream().map(DepartmentEntity::getCode).toList()))
            );
        }

        return new ResolvedAssignments(
                roleCodes,
                moduleCodes,
                departmentCodes,
                roles,
                modules,
                departments
        );
    }

    private void saveAssignments(UUID userId, OffsetDateTime now, ResolvedAssignments assignments) {
        for (RoleEntity role : assignments.roles()) {
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRole.setAssignedAt(now);
            userRoleRepo.save(userRole);
        }
        for (ModuleEntity module : assignments.modules()) {
            UserModuleEntity userModule = new UserModuleEntity();
            userModule.setUserId(userId);
            userModule.setModuleId(module.getId());
            userModule.setAssignedAt(now);
            userModuleRepo.save(userModule);
        }
        for (DepartmentEntity department : assignments.departments()) {
            UserDepartmentEntity userDepartment = new UserDepartmentEntity();
            userDepartment.setUserId(userId);
            userDepartment.setDepartmentId(department.getId());
            userDepartment.setAssignedAt(now);
            userDepartmentRepo.save(userDepartment);
        }
    }

    private void replaceAssignments(UUID userId, OffsetDateTime now, ResolvedAssignments assignments) {
        userRoleRepo.deleteByUserId(userId);
        userModuleRepo.deleteByUserId(userId);
        userDepartmentRepo.deleteByUserId(userId);
        saveAssignments(userId, now, assignments);
    }

    private void ensureUniqueUsername(String username, UUID excludedUserId) {
        userRepo.findByUsername(username).ifPresent(existing -> {
            if (excludedUserId == null || !existing.getId().equals(excludedUserId)) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR,
                        HttpStatus.CONFLICT,
                        "Username already exists.",
                        Map.of("field", "username")
                );
            }
        });
    }

    private void ensureUniqueEmail(String email, UUID excludedUserId) {
        if (email == null) {
            return;
        }
        userRepo.findByEmail(email).ifPresent(existing -> {
            if (excludedUserId == null || !existing.getId().equals(excludedUserId)) {
                throw new ApiException(
                        ErrorCode.VALIDATION_ERROR,
                        HttpStatus.CONFLICT,
                        "Email already exists.",
                        Map.of("field", "email")
                );
            }
        });
    }

    private void validateSelfUpdateSafety(
            UUID actorUserId,
            UUID targetUserId,
            UserEntity.UserStatus newStatus,
            List<String> roleCodes
    ) {
        if (!actorUserId.equals(targetUserId)) {
            return;
        }
        if (newStatus != UserEntity.UserStatus.ACTIVE) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "You cannot lock or disable your own account.",
                    Map.of("status", newStatus.name())
            );
        }
        if (!roleCodes.contains("ADMIN")) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "You cannot remove ADMIN role from your own account.",
                    Map.of("roles", roleCodes)
            );
        }
    }

    private UserEntity requiredUser(UUID userId) {
        return userRepo.findById(userId).orElseThrow(() -> new ApiException(
                ErrorCode.VALIDATION_ERROR,
                HttpStatus.NOT_FOUND,
                "User not found.",
                Map.of("userId", userId.toString())
        ));
    }

    private static UserEntity.UserStatus normalizeStatus(String rawStatus) {
        String value = rawStatus == null ? "" : rawStatus.trim().toUpperCase();
        try {
            return UserEntity.UserStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Unknown user status.",
                    Map.of("status", value, "allowed", List.of("ACTIVE", "LOCKED", "DISABLED"))
            );
        }
    }

    private static String normalizeUsername(String rawUsername) {
        String normalized = rawUsername == null ? "" : rawUsername.trim();
        if (normalized.isBlank()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Username is required.",
                    Map.of("field", "username")
            );
        }
        return normalized;
    }

    private static String normalizeEmail(String rawEmail) {
        if (rawEmail == null) {
            return null;
        }
        String normalized = rawEmail.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static Set<String> toNormalizedSet(List<String> values, boolean upper) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String cleaned = value.trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            normalized.add(upper ? cleaned.toUpperCase() : cleaned.toLowerCase());
        }
        return normalized;
    }

    private static AdminUserResponse toResponse(UserEntity user, UserAuthorizationContext context) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus().name(),
                context.roles(),
                context.modules(),
                context.departments(),
                context.permissions()
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

    private record ResolvedAssignments(
            List<String> roleCodes,
            List<String> moduleCodes,
            List<String> departmentCodes,
            List<RoleEntity> roles,
            List<ModuleEntity> modules,
            List<DepartmentEntity> departments
    ) {
    }
}
