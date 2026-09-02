package in.gov.ipie.service.user.dto.response;

import java.util.UUID;

public record InitiatePillarLinkResponse(UUID linkRequestId, String authorizationUrl) {
}
