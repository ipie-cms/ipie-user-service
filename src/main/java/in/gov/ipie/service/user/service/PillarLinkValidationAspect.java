package in.gov.ipie.service.user.service;

import java.util.UUID;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import in.gov.ipie.service.user.exception.PillarAlreadyLinkedException;
import in.gov.ipie.service.user.domain.PillarType;
import in.gov.ipie.service.user.repository.PillarLinkRepository;

/**
 * Duplicate-link guard for {@link PillarLinkServiceImpl#initiateLink}, run before the method
 * body via an AspectJ pointcut rather than an inline {@code if}/{@code throw} check. Only covers
 * {@code initiateLink} - {@code completeLinkCallback}'s equivalent guard depends on data
 * (the exchanged OAuth claims) not available until partway through that method, so it stays
 * inline there; see {@link PillarLinkServiceImpl}'s Javadoc.
 */
@Aspect
@Component
class PillarLinkValidationAspect {

    private final PillarLinkRepository pillarLinkRepository;

    PillarLinkValidationAspect(PillarLinkRepository pillarLinkRepository) {
        this.pillarLinkRepository = pillarLinkRepository;
    }

    @Before("execution(* in.gov.ipie.service.user.service.PillarLinkServiceImpl.initiateLink(..)) "
            + "&& args(userId, pillarType)")
    void validateInitiateLink(UUID userId, PillarType pillarType) {
        if (pillarLinkRepository.existsByUserIdAndPillarType(userId, pillarType)) {
            throw new PillarAlreadyLinkedException(pillarType);
        }
    }
}
