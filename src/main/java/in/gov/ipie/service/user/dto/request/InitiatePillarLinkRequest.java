package in.gov.ipie.service.user.dto.request;

import jakarta.validation.constraints.NotNull;

import in.gov.ipie.service.user.domain.PillarType;

public record InitiatePillarLinkRequest(@NotNull PillarType pillarType) {
}
