package aaa.predictive_maintenance_p.model;

import jakarta.validation.constraints.NotBlank;

public record KakaoRuntimeConfigRequest(
        @NotBlank String accessToken
) {
}
