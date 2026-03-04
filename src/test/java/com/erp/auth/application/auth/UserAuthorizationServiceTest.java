package com.erp.auth.application.auth;

import com.erp.auth.infrastructure.persistence.jpa.repository.PermissionJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserDepartmentJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserModuleJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserRoleJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthorizationServiceTest {

    @Mock
    private UserRoleJpaRepository userRoleRepo;
    @Mock
    private UserModuleJpaRepository userModuleRepo;
    @Mock
    private UserDepartmentJpaRepository userDepartmentRepo;
    @Mock
    private PermissionJpaRepository permissionRepo;

    @Test
    void resolvesAndFiltersPermissionsByAssignedModules() {
        UUID userId = UUID.randomUUID();
        UserAuthorizationService service = new UserAuthorizationService(
                userRoleRepo, userModuleRepo, userDepartmentRepo, permissionRepo
        );

        when(userRoleRepo.findRoleCodesByUserId(userId)).thenReturn(List.of("ADMIN", "USER"));
        when(userModuleRepo.findModuleCodesByUserId(userId)).thenReturn(List.of("SALES"));
        when(userDepartmentRepo.findDepartmentCodesByUserId(userId)).thenReturn(List.of("HQ"));
        when(permissionRepo.findPermissionClaimsByUserId(userId)).thenReturn(List.of(
                projection("admin.seats.read", null),
                projection("sales.order.read", "SALES"),
                projection("inventory.stock.read", "INVENTORY")
        ));

        UserAuthorizationContext context = service.resolveForUser(userId);

        assertEquals(List.of("ADMIN", "USER"), context.roles());
        assertEquals(List.of("SALES"), context.modules());
        assertEquals(List.of("HQ"), context.departments());
        assertEquals(List.of("admin.seats.read", "sales.order.read"), context.permissions());
    }

    private static PermissionJpaRepository.PermissionClaimProjection projection(String code, String moduleCode) {
        return new PermissionJpaRepository.PermissionClaimProjection() {
            @Override
            public String getCode() {
                return code;
            }

            @Override
            public String getModuleCode() {
                return moduleCode;
            }
        };
    }
}
