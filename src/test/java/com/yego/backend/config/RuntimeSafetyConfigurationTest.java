package com.yego.backend.config;

import com.yego.backend.scheduler.yego_marketing_mensajes.MarketingMensajeScheduler;
import com.yego.backend.scheduler.yego_marketing_mensajes.WhatsAppGroupSyncScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeSafetyConfigurationTest {

    @Test
    void passwordEncoderViveFueraDeSecurityConfig() {
        PasswordEncoder encoder = new PasswordEncoderConfig().passwordEncoder(12);

        assertThat(encoder.matches("Clave-Segura-123!", encoder.encode("Clave-Segura-123!")))
                .isTrue();
        assertThat(Arrays.stream(SecurityConfig.class.getDeclaredMethods())
                .map(Method::getName))
                .doesNotContain("passwordEncoder");
    }

    @Test
    void tareasDeMarketingSoloSeRegistranEnProduccion() {
        assertProdProfile(MarketingMensajeScheduler.class);
        assertProdProfile(WhatsAppGroupSyncScheduler.class);
    }

    private static void assertProdProfile(Class<?> componentType) {
        Profile profile = componentType.getAnnotation(Profile.class);
        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("prod");
    }
}
