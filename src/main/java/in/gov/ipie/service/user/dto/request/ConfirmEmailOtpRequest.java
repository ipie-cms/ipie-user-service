package in.gov.ipie.service.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmEmailOtpRequest(

        @NotBlank
        @Size(max = 10)
        String code) {
}
