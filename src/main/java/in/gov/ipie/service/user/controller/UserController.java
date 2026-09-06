package in.gov.ipie.service.user.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.common.security.context.CurrentUserProvider;
import in.gov.ipie.common.security.permission.RequiresPermission;
import in.gov.ipie.common.web.idempotency.Idempotent;
import in.gov.ipie.common.web.paging.CursorPageResponse;
import in.gov.ipie.common.web.paging.PageResponse;
import in.gov.ipie.service.user.permission.UserPermissions;
import in.gov.ipie.service.user.mapper.UserApiMapper;
import in.gov.ipie.service.user.dto.request.AffiliateOrganisationRequest;
import in.gov.ipie.service.user.dto.request.CreateUserRequest;
import in.gov.ipie.service.user.dto.request.UpdateNotificationChannelsRequest;
import in.gov.ipie.service.user.dto.request.UpdateUserRequest;
import in.gov.ipie.service.user.dto.response.UserResponse;
import in.gov.ipie.service.user.service.UserService;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.domain.UserSearchCriteria;
import in.gov.ipie.service.user.domain.UserSortField;
import in.gov.ipie.service.user.domain.UserStatus;
import in.gov.ipie.service.user.service.VisibilityScopeResolver;

/**
 * User CRUD API - the reference vertical slice this template ships with. Only HTTP concerns live
 * here: request/response mapping and status codes; every business rule lives in {@link
 * UserService} or the domain model (master standards doc, 5.1/5.2: "Keep controllers thin").
 * Permission checks ({@link RequiresPermission}) and idempotency ({@link Idempotent}) are AOP
 * advice, not inline code - see {@code PermissionCheckAspect}/{@code IdempotencyAspect}.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserApiMapper userApiMapper;
    private final CurrentUserProvider currentUserProvider;
    private final VisibilityScopeResolver visibilityScopeResolver;

    public UserController(UserService userService, UserApiMapper userApiMapper,
            CurrentUserProvider currentUserProvider, VisibilityScopeResolver visibilityScopeResolver) {
        this.userService = userService;
        this.userApiMapper = userApiMapper;
        this.currentUserProvider = currentUserProvider;
        this.visibilityScopeResolver = visibilityScopeResolver;
    }

    /**
     * Resolves the authenticated caller by the JWT {@code sub} claim (the Keycloak user id) - the
     * dashboard in ipie-web calls this to decide whether to show role-based content or "Please
     * contact administrator" (see {@code User.RegistrationStatus}).
     */
    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        UUID keycloakUserId = UUID.fromString(currentUserProvider.currentOrThrow().userId());
        return userApiMapper.toResponse(userService.getCurrentUser(keycloakUserId));
    }

    @PostMapping
    @RequiresPermission(UserPermissions.USER_WRITE)
    @Idempotent
    public ResponseEntity<Object> createUser(@Valid @RequestBody CreateUserRequest request) {
        User created = userService.createUser(userApiMapper.toCommand(request));
        UserResponse response = userApiMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.getId())).body(response);
    }

    @GetMapping("/{id}")
    @RequiresPermission(UserPermissions.USER_READ)
    public UserResponse getUser(@PathVariable UUID id) {
        return userApiMapper.toResponse(userService.getUser(id));
    }

    @GetMapping
    @RequiresPermission(UserPermissions.USER_READ)
    public PageResponse<UserResponse> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UserSortField sortBy,
            @RequestParam(defaultValue = "ASC") PageRequest.SortDirection sortDirection) {
        UserSearchCriteria criteria = new UserSearchCriteria(username, email, status);
        String sortProperty = sortBy == null ? null : sortBy.propertyName();
        PageRequest pageRequest = new PageRequest(page, size, sortProperty, sortDirection);

        PageResult<User> result = userService.searchUsers(criteria, visibilityScopeResolver.forCurrentUser(), pageRequest);
        return PageResponse.from(result, userApiMapper::toResponse);
    }

    /**
     * Keyset ("seek") variant of {@link #searchUsers} - no total count, but stays fast at any
     * depth (master standards doc, section 8). Prefer this for large/high-traffic listings;
     * {@link #searchUsers} remains for admin-style screens that need page numbers and a total count.
     */
    @GetMapping("/cursor")
    @RequiresPermission(UserPermissions.USER_READ)
    public CursorPageResponse<UserResponse> searchUsersAfter(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        UserSearchCriteria criteria = new UserSearchCriteria(username, email, status);
        CursorPageRequest pageRequest = new CursorPageRequest(cursor, size);

        CursorPageResult<User> result = userService.searchUsersAfter(criteria, visibilityScopeResolver.forCurrentUser(), pageRequest);
        return CursorPageResponse.from(result, userApiMapper::toResponse);
    }

    @PutMapping("/{id}")
    @RequiresPermission(UserPermissions.USER_WRITE)
    public UserResponse updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        User updated = userService.updateUser(userApiMapper.toCommand(id, request));
        return userApiMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(UserPermissions.USER_DELETE)
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id, @RequestParam(required = false) String comment) {
        userService.deactivateUser(id, comment);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    @RequiresPermission(UserPermissions.USER_WRITE)
    public UserResponse reactivateUser(@PathVariable UUID id, @RequestParam(required = false) String comment) {
        return userApiMapper.toResponse(userService.reactivateUser(id, comment));
    }

    /** {@code request.organisationId()} may be {@code null} to un-affiliate (FRS 1.1.1). */
    @PutMapping("/{id}/organisation")
    @RequiresPermission(UserPermissions.USER_WRITE)
    public UserResponse affiliateWithOrganisation(@PathVariable UUID id, @RequestBody AffiliateOrganisationRequest request) {
        UUID organisationId = request.organisationId() == null ? null : UUID.fromString(request.organisationId());
        return userApiMapper.toResponse(userService.affiliateWithOrganisation(id, organisationId, request.comment()));
    }

    @PatchMapping("/{id}/notification-channels")
    @RequiresPermission(UserPermissions.USER_WRITE)
    public UserResponse updateNotificationChannels(@PathVariable UUID id, @Valid @RequestBody UpdateNotificationChannelsRequest request) {
        User updated = userService.updateNotificationChannels(id, request.notificationChannels(), request.comment());
        return userApiMapper.toResponse(updated);
    }
}
