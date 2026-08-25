package aaa.predictive_maintenance_p.service;

import aaa.predictive_maintenance_p.model.DemoKakaoNotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KakaoMessageServiceTest {

    private final DemoKakaoNotificationRequest request = new DemoKakaoNotificationRequest(
            "FAILURE_EXPECTED-1786464000000",
            "DEMO-ANT",
            "FAILURE_EXPECTED",
            1786464000000L,
            1786464000000L
    );

    @Test
    void disabledConfigurationNeverSends() {
        KakaoMessageService service = service(false, false);

        KakaoMessageService.SendResult result = service.sendDemoAlert(request);

        assertThat(result.status()).isEqualTo("NOT_CONFIGURED");
    }

    @Test
    void dryRunNeverSends() {
        KakaoMessageService service = service(true, true);

        KakaoMessageService.SendResult result = service.sendDemoAlert(request);

        assertThat(result.status()).isEqualTo("DRY_RUN");
    }

    private KakaoMessageService service(boolean enabled, boolean dryRun) {
        KakaoOAuthService oauthService = mock(KakaoOAuthService.class);
        when(oauthService.validTokens()).thenReturn(List.of());
        KakaoMessageService service = new KakaoMessageService(
                RestClient.builder(), new ObjectMapper(), oauthService
        );
        ReflectionTestUtils.setField(service, "enabled", enabled);
        ReflectionTestUtils.setField(service, "dryRun", dryRun);
        ReflectionTestUtils.setField(service, "accessToken", "");
        ReflectionTestUtils.setField(service, "allowedUserId", "");
        ReflectionTestUtils.setField(service, "dashboardUrl", "http://localhost:5173");
        return service;
    }
}
