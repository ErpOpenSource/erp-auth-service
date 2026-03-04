package com.erp.auth.application.auth;

import com.erp.auth.infrastructure.persistence.jpa.repository.PermissionJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserDepartmentJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserModuleJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserRoleJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserAuthorizationService {

    private final UserRoleJpaRepository userRoleRepo;
    private final UserModuleJpaRepository userModuleRepo;
    private final UserDepartmentJpaRepository userDepartmentRepo;
    private final PermissionJpaRepository permissionRepo;

    public UserAuthorizationService(
            UserRoleJpaRepository userRoleRepo,
            UserModuleJpaRepository userModuleRepo,
            UserDepartmentJpaRepository userDepartmentRepo,
            PermissionJpaRepository permissionRepo
    ) {
        this.userRoleRepo = userRoleRepo;
        this.userModuleRepo = userModuleRepo;
        this.userDepartmentRepo = userDepartmentRepo;
        this.permissionRepo = permissionRepo;
    }

    @Transactional(readOnly = true)
    public UserAuthorizationContext resolveForUser(UUID userId) {
        List<String> roles = userRoleRepo.findRoleCodesByUserId(userId);
        List<String> modules = userModuleRepo.findModuleCodesByUserId(userId);
        List<String> departments = userDepartmentRepo.findDepartmentCodesByUserId(userId);

        Set<String> moduleSet = new LinkedHashSet<>(modules);
        List<PermissionJpaRepository.PermissionClaimProjection> claims = permissionRepo.findPermissionClaimsByUserId(userId);
        LinkedHashSet<String> permissions = new LinkedHashSet<>();
        for (PermissionJpaRepository.PermissionClaimProjection claim : claims) {
            String moduleCode = claim.getModuleCode();
            if (moduleCode == null || moduleSet.contains(moduleCode)) {
                permissions.add(claim.getCode());
            }
        }

        return new UserAuthorizationContext(
                List.copyOf(new LinkedHashSet<>(roles)),
                List.copyOf(new LinkedHashSet<>(modules)),
                List.copyOf(new LinkedHashSet<>(departments)),
                new ArrayList<>(permissions)
        );
    }
}
