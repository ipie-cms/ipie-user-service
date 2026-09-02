package in.gov.ipie.service.user.persistence;

import java.util.Optional;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import in.gov.ipie.service.user.domain.PillarLinkRequest;
import in.gov.ipie.service.user.repository.PillarLinkRequestRepository;

@Repository
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class PillarLinkRequestRepositoryImpl implements PillarLinkRequestRepository {

    private final PillarLinkRequestJpaRepository jpaRepository;
    private final PillarLinkPersistenceMapper mapper;

    @Override
    public PillarLinkRequest save(PillarLinkRequest request) {
        if (request.getId() == null) {
            return mapper.toDomain(jpaRepository.save(mapper.toNewEntity(request)));
        }
        PillarLinkRequestJpaEntity entity = jpaRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalStateException("PillarLinkRequest " + request.getId() + " disappeared mid-transaction"));
        mapper.copyMutableFieldsOnto(request, entity);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<PillarLinkRequest> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }
}
