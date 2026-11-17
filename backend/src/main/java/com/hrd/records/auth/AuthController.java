package com.hrd.records.auth;

import com.hrd.records.security.JwtService;
import com.hrd.records.security.UserPrincipal;
import com.hrd.records.user.UserDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login and token issuing. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange username/password for a JWT")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal.user().getUsername(),
                principal.user().getRole().name(), principal.user().getFullName());
        return new AuthDtos.LoginResponse(token, "Bearer", jwtService.ttl().toSeconds(),
                UserDtos.UserSummary.of(principal.user()));
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated user")
    public UserDtos.UserSummary me(@AuthenticationPrincipal UserPrincipal principal) {
        return UserDtos.UserSummary.of(principal.user());
    }
}
