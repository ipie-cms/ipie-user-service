package in.gov.ipie.service.user.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.common.security.permission.RequiresPermission;
import in.gov.ipie.common.web.idempotency.Idempotent;
import in.gov.ipie.common.web.paging.PageResponse;
import in.gov.ipie.service.user.domain.Organisation;
import in.gov.ipie.service.user.dto.request.CreateOrganisationRequest;
import in.gov.ipie.service.user.dto.request.UpdateOrganisationRequest;
import in.gov.ipie.service.user.dto.response.OrganisationResponse;
import in.gov.ipie.service.user.mapper.OrganisationApiMapper;
import in.gov.ipie.service.user.permission.OrganisationPermissions;
import in.gov.ipie.service.user.service.OrganisationService;

/**
 * Organisation API (FRS 1.1.1's "Entity") - find-or-create, not a strict create, since the
 * platform must never hold two rows for the same government-issued id (see {@link
 * OrganisationService#findOrCreate}'s Javadoc). Only HTTP concerns live here, same convention as
 * {@link UserController}.
 */
@RestController
@RequestMapping("/api/v1/organisations")
public class OrganisationController {

    private final OrganisationService organisationService;
    private final OrganisationApiMapper organisationApiMapper;

    public OrganisationController(OrganisationService organisationService, OrganisationApiMapper organisationApiMapper) {
        this.organisationService = organisationService;
        this.organisationApiMapper = organisationApiMapper;
    }

    @PostMapping
    @RequiresPermission(OrganisationPermissions.ORGANISATION_WRITE)
    @Idempotent
    public OrganisationResponse findOrCreateOrganisation(@Valid @RequestBody CreateOrganisationRequest request) {
        Organisation organisation = organisationService.findOrCreate(organisationApiMapper.toCommand(request));
        return organisationApiMapper.toResponse(organisation);
    }

    @GetMapping("/{id}")
    @RequiresPermission(OrganisationPermissions.ORGANISATION_READ)
    public OrganisationResponse getOrganisation(@PathVariable UUID id) {
        return organisationApiMapper.toResponse(organisationService.getOrganisation(id));
    }

    @GetMapping
    @RequiresPermission(OrganisationPermissions.ORGANISATION_READ)
    public PageResponse<OrganisationResponse> searchOrganisations(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Organisation> result = organisationService.searchOrganisations(name, PageRequest.of(page, size));
        return PageResponse.from(result, organisationApiMapper::toResponse);
    }

    @PutMapping("/{id}")
    @RequiresPermission(OrganisationPermissions.ORGANISATION_WRITE)
    public OrganisationResponse updateOrganisation(@PathVariable UUID id, @Valid @RequestBody UpdateOrganisationRequest request) {
        Organisation updated = organisationService.updateOrganisation(organisationApiMapper.toCommand(id, request));
        return organisationApiMapper.toResponse(updated);
    }
}
