package com.yego.backend.config;

import com.yego.backend.entity.yego_principal.api.response.UserResponseDto;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECURE_TEST_SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void rechazaClaveInsuficienteParaHs512() {
        assertThatThrownBy(() -> new JwtTokenProvider("clave-corta", 3600))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("64 bytes");
    }

    @Test
    void generaYValidaTokenConClaimsRequeridos() {
        JwtTokenProvider provider = new JwtTokenProvider(SECURE_TEST_SECRET, 3600);
        UserResponseDto user = UserResponseDto.builder()
                .id(42L)
                .username("usuario")
                .roleName("ADMIN")
                .build();

        String token = provider.generate(user);

        assertThat(provider.parse(token).getSubject()).isEqualTo("42");
        assertThat(provider.parse(token).get("username", String.class)).isEqualTo("usuario");
        assertThat(provider.parse(token).get("userId", Long.class)).isEqualTo(42L);
        assertThat(provider.parse(token).getExpiration()).isAfter(provider.parse(token).getIssuedAt());
    }

    @Test
    void generaTokenTipadoParaMovilODispositivoConExpiracion() {
        JwtTokenProvider provider = new JwtTokenProvider(SECURE_TEST_SECRET, 3600);

        String token = provider.generate(
                "driver-42",
                Map.of("driverId", "driver-42", "type", "mobile_driver"),
                600);

        assertThat(provider.parse(token).getSubject()).isEqualTo("driver-42");
        assertThat(provider.parse(token).get("type", String.class)).isEqualTo("mobile_driver");
        assertThat(provider.parse(token).getExpiration()).isAfter(provider.parse(token).getIssuedAt());
    }

    @Test
    void rechazaTokenPersonalizadoSinExpiracionValida() {
        JwtTokenProvider provider = new JwtTokenProvider(SECURE_TEST_SECRET, 3600);

        assertThatThrownBy(() -> provider.generate("device:1", Map.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiración");
    }
}
