package com.offerpilot.api;

import com.offerpilot.domain.AppUser;
import com.offerpilot.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AppUserRepository users;
    private final PasswordEncoder passwords;
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    public AuthController(AppUserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody RegisterRequest r, HttpServletRequest request, HttpServletResponse response) {
        String username = r.username().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(username)) throw new IllegalArgumentException("用户名已存在");
        AppUser user = users.save(new AppUser(username, r.displayName().trim(), passwords.encode(r.password())));
        signIn(user, request, response);
        return view(user);
    }

    @PostMapping("/login")
    public UserView login(@Valid @RequestBody LoginRequest r, HttpServletRequest request, HttpServletResponse response) {
        AppUser user = users.findByUsernameIgnoreCase(r.username().trim())
                .orElseThrow(() -> new BadCredentialsException("用户名或密码错误"));
        if (!passwords.matches(r.password(), user.getPasswordHash()))
            throw new BadCredentialsException("用户名或密码错误");
        signIn(user, request, response);
        return view(user);
    }

    @GetMapping("/me")
    public UserView me(Authentication authentication) {
        AppUser user = users.findById(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new BadCredentialsException("用户不存在"));
        return view(user);
    }

    private void signIn(AppUser user, HttpServletRequest request, HttpServletResponse response) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(user.getId().toString(), null, List.of());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
    }

    private UserView view(AppUser user) {
        return new UserView(user.getId().toString(), user.getUsername(), user.getDisplayName());
    }

    public record RegisterRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,30}") String username,
                                  @NotBlank @Size(max = 80) String displayName,
                                  @NotBlank @Size(min = 8, max = 72) String password) {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record UserView(String id, String username, String displayName) {}
}
