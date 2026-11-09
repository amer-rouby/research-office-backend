package com.researchoffice.identity.user;

import com.researchoffice.identity.Role;
import com.researchoffice.identity.user.StaffService.CreateStaffRequest;
import com.researchoffice.identity.user.StaffService.PasswordRequest;
import com.researchoffice.identity.user.StaffService.StaffView;
import com.researchoffice.identity.user.StaffService.UpdateStaffRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
class StaffController {

    private final StaffService staff;

    @GetMapping
    List<StaffView> list(@RequestParam(required = false) Role role) {
        return staff.list(role);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StaffView create(@Valid @RequestBody CreateStaffRequest request) {
        return staff.create(request);
    }

    @PutMapping("/{id}")
    StaffView update(@PathVariable UUID id, @Valid @RequestBody UpdateStaffRequest request,
                     @AuthenticationPrincipal Jwt jwt) {
        return staff.update(id, request, UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void password(@PathVariable UUID id, @Valid @RequestBody PasswordRequest request) {
        staff.changePassword(id, request.password());
    }
}
