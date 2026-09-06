package in.gov.ipie.service.user.dto.response;

import java.time.Instant;
import java.util.UUID;

import in.gov.ipie.service.user.domain.PillarType;

public record PillarLinkResponse(
        UUID id,
        PillarType pillarType,
        String externalUsername,
        Instant linkedAt) {
}
