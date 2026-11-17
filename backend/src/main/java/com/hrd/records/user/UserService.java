package com.hrd.records.user;

import com.hrd.records.audit.AuditAction;
import com.hrd.records.audit.AuditService;
import com.hrd.records.common.ConflictException;
import com.hrd.records.common.NotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User administration. Every mutation is written to the audit trail, and the
 * last enabled ADMIN is protected from demotion, disabling and deletion so the
 * system can never lock itself out.
 */
@Service
public class UserService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserAccountRepository repository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserSummary> list() {
        return repository.findAll().stream().map(UserDtos.UserSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public UserDtos.UserSummary get(Long id) {
        return UserDtos.UserSummary.of(requireUser(id));
    }

    @Transactional
    public UserDtos.UserSummary create(UserDtos.CreateUserRequest request) {
        if (repository.existsByUsername(request.username())) {
            throw new ConflictException("Username '" + request.username() + "' is already taken");
        }
        UserAccount user = new UserAccount(request.username(), passwordEncoder.encode(request.password()),
                request.fullName(), request.role());
        UserAccount saved = repository.save(user);
        UserDtos.UserSummary summary = UserDtos.UserSummary.of(saved);
        auditService.record(AuditAction.CREATE, "UserAccount", String.valueOf(saved.getId()), null, summary);
        return summary;
    }

    @Transactional
    public UserDtos.UserSummary update(Long id, UserDtos.UpdateUserRequest request) {
        UserAccount user = requireUser(id);
        UserDtos.UserSummary before = UserDtos.UserSummary.of(user);
        guardLastAdmin(before.role(), before.enabled(), request.role(), request.enabled());
        user.setFullName(request.fullName());
        user.setRole(request.role());
        user.setEnabled(request.enabled());
        UserDtos.UserSummary after = UserDtos.UserSummary.of(user);
        auditService.record(AuditAction.UPDATE, "UserAccount", String.valueOf(user.getId()), before, after);
        return after;
    }

    @Transactional
    public void delete(Long id) {
        UserAccount user = requireUser(id);
        if (user.getUsername().equals(currentUsername())) {
            throw new ConflictException("You cannot delete your own account");
        }
        if (user.getRole() == Role.ADMIN && user.isEnabled()
                && repository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new ConflictException("Cannot delete the last enabled ADMIN account");
        }
        auditService.record(AuditAction.DELETE, "UserAccount", String.valueOf(user.getId()), UserDtos.UserSummary.of(user), null);
        repository.delete(user);
    }

    /** Blocks removing ADMIN powers from the sole enabled ADMIN account. */
    private void guardLastAdmin(Role oldRole, boolean oldEnabled, Role newRole, boolean newEnabled) {
        boolean losesAdmin = oldRole == Role.ADMIN && oldEnabled && (newRole != Role.ADMIN || !newEnabled);
        if (losesAdmin && repository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new ConflictException("Cannot remove the last enabled ADMIN account");
        }
    }

    private UserAccount requireUser(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
