package com.erp.auth.application.admin;

import com.erp.auth.application.audit.AuditService;
import com.erp.auth.application.auth.UserAuthorizationContext;
import com.erp.auth.application.auth.UserAuthorizationService;
import com.erp.auth.infrastructure.persistence.jpa.entity.DepartmentEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.ModuleEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.PermissionEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.RoleEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.RolePermissionEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserDepartmentEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserModuleEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.DepartmentJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.ModuleJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.PermissionJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.RoleJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.RolePermissionJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserDepartmentJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserModuleJpaRepository;
import com.erp.auth.interfaces.api.dto.AdminCodeNameResponse;
import com.erp.auth.interfaces.api.dto.AdminCreateDepartmentRequest;
import com.erp.auth.interfaces.api.dto.AdminCreateModuleRequest;
import com.erp.auth.interfaces.api.dto.AdminCreatePermissionRequest;
import com.erp.auth.interfaces.api.dto.AdminDepartmentResponse;
import com.erp.auth.interfaces.api.dto.AdminUserAccessResponse;
import com.erp.auth.interfaces.api.dto.UpdateDepartmentRequest;
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
public class AdminAuthorizationUseCase {

    private final UserJpaRepository userRepo;
    private final RoleJpaRepository roleRepo;
    private final PermissionJpaRepository permissionRepo;
    private final RolePermissionJpaRepository rolePermissionRepo;
    private final ModuleJpaRepository moduleRepo;
    private final DepartmentJpaRepository departmentRepo;
    private final UserModuleJpaRepository userModuleRepo;
    private final UserDepartmentJpaRepository userDepartmentRepo;
    private final UserAuthorizationService userAuthorizationService;
    private final AuditService auditService;

    public AdminAuthorizationUseCase(
            UserJpaRepository userRepo,
            RoleJpaRepository roleRepo,
            PermissionJpaRepository permissionRepo,
            RolePermissionJpaRepository rolePermissionRepo,
            ModuleJpaRepository moduleRepo,
            DepartmentJpaRepository departmentRepo,
            UserModuleJpaRepository userModuleRepo,
            UserDepartmentJpaRepository userDepartmentRepo,
            UserAuthorizationService userAuthorizationService,
            AuditService auditService
    ) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.permissionRepo = permissionRepo;
        this.rolePermissionRepo = rolePermissionRepo;
        this.moduleRepo = moduleRepo;
        this.departmentRepo = departmentRepo;
        this.userModuleRepo = userModuleRepo;
        this.userDepartmentRepo = userDepartmentRepo;
        this.userAuthorizationService = userAuthorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public AdminCodeNameResponse createModule(UUID actorUserId, AdminCreateModuleRequest request, String ip, String userAgent) {
        String code = normalizeUpper(request.code());
        if (moduleRepo.findByCode(code).isPresent()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT, "Module already exists.", Map.of("code", code));
        }
        ModuleEntity module = new ModuleEntity();
        module.setId(UUID.randomUUID());
        module.setCode(code);
        module.setName(request.name().trim());
        module.setCreatedAt(OffsetDateTime.now());
        moduleRepo.save(module);

        auditService.record("ADMIN_MODULE_CREATE", actor(actorUserId), null, null,
                "{\"code\":\"" + safe(code) + "\",\"name\":\"" + safe(module.getName()) + "\",\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}");
        return new AdminCodeNameResponse(module.getCode(), module.getName());
    }

    @Transactional(readOnly = true)
    public List<AdminCodeNameResponse> listRoles() {
        return roleRepo.findAll(org.springframework.data.domain.Sort.by("code"))
                .stream()
                .map(r -> new AdminCodeNameResponse(r.getCode(), r.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminCodeNameResponse> listPermissions() {
        return permissionRepo.findAll(org.springframework.data.domain.Sort.by("code"))
                .stream()
                .map(p -> new AdminCodeNameResponse(p.getCode(), p.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getRolePermissions(String roleCode) {
        RoleEntity role = roleRepo.findByCode(normalizeUpper(roleCode))
                .orElseThrow(() -> new ApiException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Role not found.",
                        Map.of("roleCode", roleCode)
                ));
        return rolePermissionRepo.findPermissionCodesByRoleId(role.getId());
    }

    @Transactional(readOnly = true)
    public List<AdminCodeNameResponse> listModuleCatalog() {
        return moduleRepo.findAll(org.springframework.data.domain.Sort.by("code"))
                .stream()
                .map(m -> new AdminCodeNameResponse(m.getCode(), m.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminDepartmentResponse> listDepartments() {
        return departmentRepo.findAll(org.springframework.data.domain.Sort.by("name"))
                .stream()
                .map(d -> new AdminDepartmentResponse(d.getId(), d.getCode(), d.getName(), d.getCreatedAt()))
                .toList();
    }

    @Transactional
    public AdminDepartmentResponse updateDepartment(UUID actorUserId, UUID departmentId, UpdateDepartmentRequest request, String ip, String userAgent) {
        DepartmentEntity department = departmentRepo.findById(departmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Department not found.", Map.of("id", departmentId.toString())));

        String newCode = normalizeUpper(request.code());
        departmentRepo.findByCode(newCode).ifPresent(existing -> {
            if (!existing.getId().equals(departmentId)) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT, "Department code already in use.", Map.of("code", newCode));
            }
        });

        department.setCode(newCode);
        department.setName(request.name().trim());
        departmentRepo.save(department);

        auditService.record("ADMIN_DEPARTMENT_UPDATE", actor(actorUserId), null, null,
                "{\"id\":\"" + departmentId + "\",\"code\":\"" + safe(newCode) + "\",\"ip\":\"" + safe(ip) + "\"}");
        return new AdminDepartmentResponse(department.getId(), department.getCode(), department.getName(), department.getCreatedAt());
    }

    @Transactional
    public void deleteDepartment(UUID actorUserId, UUID departmentId, String ip, String userAgent) {
        DepartmentEntity department = departmentRepo.findById(departmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Department not found.", Map.of("id", departmentId.toString())));
        departmentRepo.delete(department);
        auditService.record("ADMIN_DEPARTMENT_DELETE", actor(actorUserId), null, null,
                "{\"id\":\"" + departmentId + "\",\"code\":\"" + safe(department.getCode()) + "\",\"ip\":\"" + safe(ip) + "\"}");
    }

    @Transactional
    public AdminCodeNameResponse createDepartment(UUID actorUserId, AdminCreateDepartmentRequest request, String ip, String userAgent) {
        String code = normalizeUpper(request.code());
        if (departmentRepo.findByCode(code).isPresent()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT, "Department already exists.", Map.of("code", code));
        }
        DepartmentEntity department = new DepartmentEntity();
        department.setId(UUID.randomUUID());
        department.setCode(code);
        department.setName(request.name().trim());
        department.setCreatedAt(OffsetDateTime.now());
        departmentRepo.save(department);

        auditService.record("ADMIN_DEPARTMENT_CREATE", actor(actorUserId), null, null,
                "{\"code\":\"" + safe(code) + "\",\"name\":\"" + safe(department.getName()) + "\",\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}");
        return new AdminCodeNameResponse(department.getCode(), department.getName());
    }

    @Transactional
    public AdminCodeNameResponse createPermission(UUID actorUserId, AdminCreatePermissionRequest request, String ip, String userAgent) {
        String code = request.code().trim().toLowerCase();
        if (permissionRepo.findByCode(code).isPresent()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, HttpStatus.CONFLICT, "Permission already exists.", Map.of("code", code));
        }

        UUID moduleId = null;
        String moduleCode = null;
        if (request.moduleCode() != null && !request.moduleCode().isBlank()) {
            String normalizedModuleCode = normalizeUpper(request.moduleCode());
            moduleCode = normalizedModuleCode;
            ModuleEntity module = moduleRepo.findByCode(normalizedModuleCode)
                    .orElseThrow(() -> new ApiException(
                            ErrorCode.VALIDATION_ERROR,
                            HttpStatus.BAD_REQUEST,
                            "Unknown module code.",
                            Map.of("moduleCode", normalizedModuleCode)
                    ));
            moduleId = module.getId();
        }

        PermissionEntity permission = new PermissionEntity();
        permission.setId(UUID.randomUUID());
        permission.setCode(code);
        permission.setName(request.name().trim());
        permission.setModuleId(moduleId);
        permission.setCreatedAt(OffsetDateTime.now());
        permissionRepo.save(permission);

        auditService.record("ADMIN_PERMISSION_CREATE", actor(actorUserId), null, null,
                "{\"code\":\"" + safe(code) + "\",\"module\":\"" + safe(moduleCode) + "\",\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}");
        return new AdminCodeNameResponse(permission.getCode(), permission.getName());
    }

    @Transactional
    public List<String> assignUserModules(UUID actorUserId, UUID userId, List<String> codes, String ip, String userAgent) {
        UserEntity targetUser = requiredUser(userId);
        List<String> normalizedCodes = normalizeUpperList(codes);
        List<ModuleEntity> modules = moduleRepo.findByCodeIn(normalizedCodes);
        if (modules.size() != normalizedCodes.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Unknown module code(s).",
                    Map.of("modules", findMissing(normalizedCodes, modules.stream().map(ModuleEntity::getCode).toList()))
            );
        }

        userModuleRepo.deleteByUserId(userId);
        OffsetDateTime now = OffsetDateTime.now();
        for (ModuleEntity module : modules) {
            UserModuleEntity row = new UserModuleEntity();
            row.setUserId(userId);
            row.setModuleId(module.getId());
            row.setAssignedAt(now);
            userModuleRepo.save(row);
        }

        auditService.record("ADMIN_USER_MODULES_ASSIGN", actor(actorUserId), targetUser, null,
                "{\"userId\":\"" + userId + "\",\"modules\":[" + toJsonArray(normalizedCodes) + "],\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}");
        return normalizedCodes;
    }

    @Transactional
    public List<String> assignUserDepartments(UUID actorUserId, UUID userId, List<String> codes, String ip, String userAgent) {
        UserEntity targetUser = requiredUser(userId);
        List<String> normalizedCodes = normalizeUpperList(codes);
        List<DepartmentEntity> departments = departmentRepo.findByCodeIn(normalizedCodes);
        if (departments.size() != normalizedCodes.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Unknown department code(s).",
                    Map.of("departments", findMissing(normalizedCodes, departments.stream().map(DepartmentEntity::getCode).toList()))
            );
        }

        userDepartmentRepo.deleteByUserId(userId);
        OffsetDateTime now = OffsetDateTime.now();
        for (DepartmentEntity department : departments) {
            UserDepartmentEntity row = new UserDepartmentEntity();
            row.setUserId(userId);
            row.setDepartmentId(department.getId());
            row.setAssignedAt(now);
            userDepartmentRepo.save(row);
        }

        auditService.record("ADMIN_USER_DEPARTMENTS_ASSIGN", actor(actorUserId), targetUser, null,
                "{\"userId\":\"" + userId + "\",\"departments\":[" + toJsonArray(normalizedCodes) + "],\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}");
        return normalizedCodes;
    }

    @Transactional
    public List<String> assignRolePermissions(UUID actorUserId, String roleCode, List<String> permissionCodes, String ip, String userAgent) {
        String normalizedRole = normalizeUpper(roleCode);
        RoleEntity role = roleRepo.findByCode(normalizedRole)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.VALIDATION_ERROR,
                        HttpStatus.NOT_FOUND,
                        "Role not found.",
                        Map.of("roleCode", normalizedRole)
                ));

        List<String> normalizedPermissions = normalizePermissionCodes(permissionCodes);
        List<PermissionEntity> permissions = permissionRepo.findByCodeIn(normalizedPermissions);
        if (permissions.size() != normalizedPermissions.size()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Unknown permission code(s).",
                    Map.of("permissions", findMissing(normalizedPermissions, permissions.stream().map(PermissionEntity::getCode).toList()))
            );
        }

        rolePermissionRepo.deleteByRoleId(role.getId());
        OffsetDateTime now = OffsetDateTime.now();
        for (PermissionEntity permission : permissions) {
            RolePermissionEntity row = new RolePermissionEntity();
            row.setRoleId(role.getId());
            row.setPermissionId(permission.getId());
            row.setAssignedAt(now);
            rolePermissionRepo.save(row);
        }

        auditService.record("ADMIN_ROLE_PERMISSIONS_ASSIGN", actor(actorUserId), null, null,
                "{\"role\":\"" + safe(normalizedRole) + "\",\"permissions\":[" + toJsonArray(normalizedPermissions) + "],\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}");
        return normalizedPermissions;
    }

    @Transactional(readOnly = true)
    public AdminUserAccessResponse getUserAccess(UUID actorUserId, UUID userId, String ip, String userAgent) {
        UserEntity targetUser = requiredUser(userId);
        UserAuthorizationContext context = userAuthorizationService.resolveForUser(userId);

        auditService.record("ADMIN_USER_ACCESS_VIEW", actor(actorUserId), targetUser, null,
                "{\"userId\":\"" + userId + "\",\"ip\":\"" + safe(ip) + "\",\"userAgent\":\"" + safe(userAgent) + "\"}");

        return new AdminUserAccessResponse(
                userId,
                context.roles(),
                context.modules(),
                context.departments(),
                context.permissions()
        );
    }

    private UserEntity actor(UUID actorUserId) {
        return userRepo.findById(actorUserId).orElse(null);
    }

    private UserEntity requiredUser(UUID userId) {
        return userRepo.findById(userId).orElseThrow(() -> new ApiException(
                ErrorCode.VALIDATION_ERROR,
                HttpStatus.NOT_FOUND,
                "User not found.",
                Map.of("userId", userId.toString())
        ));
    }

    private static String normalizeUpper(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase();
    }

    private static List<String> normalizeUpperList(List<String> rawCodes) {
        if (rawCodes == null || rawCodes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String raw : rawCodes) {
            String code = normalizeUpper(raw);
            if (!code.isBlank()) {
                normalized.add(code);
            }
        }
        return List.copyOf(normalized);
    }

    private static List<String> normalizePermissionCodes(List<String> rawCodes) {
        if (rawCodes == null || rawCodes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String raw : rawCodes) {
            if (raw == null) {
                continue;
            }
            String code = raw.trim().toLowerCase();
            if (!code.isBlank()) {
                normalized.add(code);
            }
        }
        return List.copyOf(normalized);
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

    private static String safe(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\"", "'");
    }
}
