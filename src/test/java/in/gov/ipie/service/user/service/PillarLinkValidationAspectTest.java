package in.gov.ipie.service.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import in.gov.ipie.service.user.exception.PillarAlreadyLinkedException;
import in.gov.ipie.service.user.domain.PillarType;
import in.gov.ipie.service.user.repository.PillarLinkRepository;

class PillarLinkValidationAspectTest {

    private final PillarLinkRepository pillarLinkRepository = mock(PillarLinkRepository.class);
    private final PillarLinkValidationAspect aspect = new PillarLinkValidationAspect(pillarLinkRepository);

    @Test
    void validateInitiateLink_throwsConflict_whenAlreadyLinked() {
        UUID userId = UUID.randomUUID();
        when(pillarLinkRepository.existsByUserIdAndPillarType(userId, PillarType.IBBI)).thenReturn(true);

        assertThatThrownBy(() -> aspect.validateInitiateLink(userId, PillarType.IBBI))
                .isInstanceOf(PillarAlreadyLinkedException.class);
    }

    @Test
    void validateInitiateLink_passes_whenNotYetLinked() {
        UUID userId = UUID.randomUUID();
        when(pillarLinkRepository.existsByUserIdAndPillarType(userId, PillarType.IBBI)).thenReturn(false);

        assertThatCode(() -> aspect.validateInitiateLink(userId, PillarType.IBBI)).doesNotThrowAnyException();
    }
}
