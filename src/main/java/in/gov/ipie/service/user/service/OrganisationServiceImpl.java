package in.gov.ipie.service.user.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.common.audit.annotation.Auditable;
import in.gov.ipie.common.audit.model.AuditEventType;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.user.command.CreateOrganisationCommand;
import in.gov.ipie.service.user.command.UpdateOrganisationCommand;
import in.gov.ipie.service.user.domain.Organisation;
import in.gov.ipie.service.user.exception.OrganisationNotFoundException;
import in.gov.ipie.service.user.repository.OrganisationRepository;

/**
 * {@link OrganisationService} implementation - business rules live here and in the domain model,
 * same convention as {@link UserServiceImpl}.
 */
@Service
public class OrganisationServiceImpl implements OrganisationService {

    private final OrganisationRepository organisationRepository;

    public OrganisationServiceImpl(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }

    @Override
    @Transactional
    @Auditable(
            action = "ORGANISATION_FOUND_OR_CREATED", entityType = "ORGANISATION", entityId = "#result.id",
            eventType = AuditEventType.BUSINESS, newValue = "#result")
    public Organisation findOrCreate(CreateOrganisationCommand command) {
        return organisationRepository.findByIdTypeAndIdValue(command.idType(), command.idValue())
                .orElseGet(() -> organisationRepository.save(Organisation.builder()
                        .name(command.name())
                        .legalConstitution(command.legalConstitution())
                        .idType(command.idType())
                        .idValue(command.idValue())
                        .msme(command.msme())
                        .msmeType(command.msmeType())
                        .registeredAddress(command.registeredAddress())
                        .contactNumber(command.contactNumber())
                        .contactEmail(command.contactEmail())
                        .country(command.country())
                        .state(command.state())
                        .city(command.city())
                        .pin(command.pin())
                        .district(command.district())
                        .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public Organisation getOrganisation(UUID organisationId) {
        return organisationRepository.findById(organisationId)
                .orElseThrow(() -> new OrganisationNotFoundException(organisationId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Organisation> searchOrganisations(String name, PageRequest pageRequest) {
        return organisationRepository.searchByName(name, pageRequest);
    }

    @Override
    @Transactional
    @Auditable(
            action = "ORGANISATION_UPDATED", entityType = "ORGANISATION", entityId = "#command.organisationId()",
            comment = "#command.comment()", newValue = "#result")
    public Organisation updateOrganisation(UpdateOrganisationCommand command) {
        Organisation organisation = organisationRepository.findById(command.organisationId())
                .orElseThrow(() -> new OrganisationNotFoundException(command.organisationId()));
        organisation.updateDetails(
                command.name(), command.legalConstitution(), command.msme(), command.msmeType(),
                command.registeredAddress(), command.contactNumber(), command.contactEmail());
        organisation.updateGeoDetails(command.country(), command.state(), command.city(), command.pin(), command.district());
        return organisationRepository.save(organisation);
    }
}
