package com.hrd.records.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** User administration - ADMIN only. */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List all users")
    public List<UserDtos.UserSummary> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single user")
    public UserDtos.UserSummary get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a user")
    public ResponseEntity<UserDtos.UserSummary> create(@Valid @RequestBody UserDtos.CreateUserRequest request) {
        UserDtos.UserSummary created = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/users/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user's name, role or enabled flag")
    public UserDtos.UserSummary update(@PathVariable Long id, @Valid @RequestBody UserDtos.UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user (never yourself or the last enabled ADMIN)")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
