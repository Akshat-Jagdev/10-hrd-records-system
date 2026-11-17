package com.hrd.records.user;

import com.hrd.records.audit.AuditAction;
import com.hrd.records.audit.AuditService;
import com.hrd.records.common.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService} with mocked persistence, encoder and
 * audit trail. The last-enabled-ADMIN protections and self-deletion guard are
 * the behaviours under scrutiny.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserAccountRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, passwordEncoder, auditService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "x"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createEncodesPasswordAndAuditsCreate() {
        when(repository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("secret-password")).thenReturn("hashed-password");
        when(repository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDtos.UserSummary summary = service.create(
                new UserDtos.CreateUserRequest("newuser", "secret-password", "New User", Role.VIEWER));

        assertThat(summary.username()).isEqualTo("newuser");
        assertThat(summary.fullName()).isEqualTo("New User");
        assertThat(summary.role()).isEqualTo(Role.VIEWER);
        assertThat(summary.enabled()).isTrue();

        ArgumentCaptor<UserAccount> savedCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getPassword()).isEqualTo("hashed-password");

        verify(auditService).record(eq(AuditAction.CREATE), eq("UserAccount"), any(), isNull(),
                any(UserDtos.UserSummary.class));
    }

    @Test
    void createDuplicateUsernameThrowsConflict() {
        when(repository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new UserDtos.CreateUserRequest("taken", "secret-password", "Duplicate", Role.VIEWER)))
                .isInstanceOf(ConflictException.class);

        verify(repository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void updateDemotingLastEnabledAdminThrowsConflict() {
        UserAccount lastAdmin = new UserAccount("lastadmin", "hash", "Last Admin", Role.ADMIN);
        when(repository.findById(1L)).thenReturn(Optional.of(lastAdmin));
        when(repository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.update(1L,
                new UserDtos.UpdateUserRequest("Last Admin", Role.HR_MANAGER, true)))
                .isInstanceOf(ConflictException.class);

        assertThat(lastAdmin.getRole()).isEqualTo(Role.ADMIN); // unchanged
        verifyNoInteractions(auditService);
    }

    @Test
    void updateDisablingLastEnabledAdminThrowsConflict() {
        UserAccount lastAdmin = new UserAccount("lastadmin", "hash", "Last Admin", Role.ADMIN);
        when(repository.findById(1L)).thenReturn(Optional.of(lastAdmin));
        when(repository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.update(1L,
                new UserDtos.UpdateUserRequest("Last Admin", Role.ADMIN, false)))
                .isInstanceOf(ConflictException.class);

        assertThat(lastAdmin.isEnabled()).isTrue(); // unchanged
        verifyNoInteractions(auditService);
    }

    @Test
    void updateDemotingSucceedsWhenAnotherEnabledAdminRemains() {
        UserAccount admin = new UserAccount("secondadmin", "hash", "Second Admin", Role.ADMIN);
        when(repository.findById(2L)).thenReturn(Optional.of(admin));
        when(repository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(2L);

        UserDtos.UserSummary summary = service.update(2L,
                new UserDtos.UpdateUserRequest("Renamed User", Role.VIEWER, true));

        assertThat(summary.fullName()).isEqualTo("Renamed User");
        assertThat(summary.role()).isEqualTo(Role.VIEWER);
        assertThat(summary.enabled()).isTrue();
        verify(auditService).record(eq(AuditAction.UPDATE), eq("UserAccount"), any(), any(),
                any(UserDtos.UserSummary.class));
    }

    @Test
    void deleteSelfThrowsConflict() {
        UserAccount self = new UserAccount("admin", "hash", "Current Admin", Role.ADMIN);
        when(repository.findById(1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class);

        verify(repository, never()).delete(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void deleteLastEnabledAdminThrowsConflict() {
        UserAccount otherAdmin = new UserAccount("otheradmin", "hash", "Other Admin", Role.ADMIN);
        when(repository.findById(5L)).thenReturn(Optional.of(otherAdmin));
        when(repository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(ConflictException.class);

        verify(repository, never()).delete(any());
        verifyNoInteractions(auditService);
    }
}
