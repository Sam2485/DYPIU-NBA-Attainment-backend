package com.dypiu.nba.security;

import com.dypiu.nba.entity.User;
import com.dypiu.nba.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * Centralized backend service to securely resolve the authenticated user's organizational scope.
 *
 * The scope is ALWAYS resolved directly from the authenticated User entity in the database.
 * Frontend query parameters (such as ?schoolId=...) are never trusted for scope resolution.
 * NO fallback to hardcoded IDs ("sch-1"), first school, first user, or arbitrary schools is permitted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentUserScopeService {

    private final UserRepository userRepository;

    /**
     * Resolves the CurrentUserScope from the active Spring SecurityContext.
     *
     * @return Immutable CurrentUserScope containing verified user and scope metadata.
     * @throws ResponseStatusException HTTP 401 if unauthenticated or user record not found; HTTP 403 if deactivated.
     */
    @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
    public CurrentUserScope getCurrentUserScope() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return resolveScopeFromAuthentication(authentication);
    }

    /**
     * Resolves the CurrentUserScope using an injected Principal or falls back to SecurityContext.
     *
     * @param principal Principal injected into controller method.
     * @return Immutable CurrentUserScope.
     */
    @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
    public CurrentUserScope getCurrentUserScope(Principal principal) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return resolveScopeByIdentifier(principal.getName());
        }
        return getCurrentUserScope();
    }

    /**
     * Resolves the CurrentUserScope from an explicit Authentication object.
     *
     * @param authentication Authentication from Spring Security.
     * @return Immutable CurrentUserScope.
     */
    @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
    public CurrentUserScope getCurrentUserScope(Authentication authentication) {
        return resolveScopeFromAuthentication(authentication);
    }

    /**
     * Retrieves the active User entity for the current authenticated principal.
     *
     * @return Managed User entity.
     * @throws ResponseStatusException HTTP 401 if unauthenticated or user not found.
     */
    @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return resolveUserFromAuthentication(authentication);
    }

    /**
     * Converts a known User entity into an immutable CurrentUserScope.
     *
     * @param user User entity.
     * @return CurrentUserScope instance.
     */
    public CurrentUserScope fromUser(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User entity is null");
        }
        return CurrentUserScope.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .schoolId(user.getSchoolId())
                .departmentId(user.getDepartmentId())
                .programmeId(user.getProgrammeId())
                .build();
    }

    private CurrentUserScope resolveScopeFromAuthentication(Authentication authentication) {
        User user = resolveUserFromAuthentication(authentication);
        return fromUser(user);
    }

    private User resolveUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user found in security context");
        }

        String identifier = authentication.getName();
        if (identifier == null || identifier.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated principal has empty identifier");
        }

        return resolveUserByIdentifier(identifier);
    }

    private CurrentUserScope resolveScopeByIdentifier(String identifier) {
        User user = resolveUserByIdentifier(identifier);
        return fromUser(user);
    }

    private User resolveUserByIdentifier(String identifier) {
        String cleanIdentifier = identifier.trim();
        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(cleanIdentifier, cleanIdentifier)
                .orElseGet(() -> userRepository.findByUsernameOrEmail(cleanIdentifier, cleanIdentifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found in database for identifier: " + cleanIdentifier)));

        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "User account is deactivated: " + cleanIdentifier);
        }

        return user;
    }
}
