package com.researchoffice.identity.user;

import com.researchoffice.identity.IdentityApi;
import com.researchoffice.identity.Role;
import com.researchoffice.shared.error.AppException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Staff accounts (admins and experts), managed by an admin. */
@Service
@RequiredArgsConstructor
@Transactional
class StaffService implements IdentityApi {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    record StaffView(UUID id, String email, String fullName, Role role, boolean active, Instant createdAt) {
        static StaffView of(User u) {
            return new StaffView(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isActive(), u.getCreatedAt());
        }
    }

    record CreateStaffRequest(@NotBlank @Email @Size(max = 190) String email,
                              @NotBlank @Size(max = 150) String fullName,
                              @NotBlank @Size(min = 10, max = 100) String password,
                              @NotNull Role role) {
    }

    record UpdateStaffRequest(@NotBlank @Size(max = 150) String fullName, boolean active) {
    }

    record PasswordRequest(@NotBlank @Size(min = 10, max = 100) String password) {
    }

    @Transactional(readOnly = true)
    List<StaffView> list(Role role) {
        return users.findAll().stream()
                .filter(u -> role == null || u.getRole() == role)
                .sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()))
                .map(StaffView::of).toList();
    }

    StaffView create(CreateStaffRequest r) {
        if (users.existsByEmail(r.email().trim())) {
            throw new AppException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "This email is already used");
        }
        return StaffView.of(users.save(new User(r.email(), r.fullName(), passwordEncoder.encode(r.password()), r.role())));
    }

    StaffView update(UUID id, UpdateStaffRequest r, UUID actingUserId) {
        User u = byId(id);
        if (id.equals(actingUserId) && !r.active()) {
            throw AppException.rule("CANNOT_DEACTIVATE_SELF", "You cannot deactivate your own account");
        }
        u.rename(r.fullName());
        u.setActive(r.active());
        return StaffView.of(u);
    }

    void changePassword(UUID id, String password) {
        byId(id).changePassword(passwordEncoder.encode(password));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StaffRef> findStaff(UUID userId) {
        return users.findById(userId).map(u -> new StaffRef(u.getId(), u.getFullName(), u.getRole(), u.isActive()));
    }

    private User byId(UUID id) {
        return users.findById(id).orElseThrow(() -> AppException.notFound("User", id));
    }
}
