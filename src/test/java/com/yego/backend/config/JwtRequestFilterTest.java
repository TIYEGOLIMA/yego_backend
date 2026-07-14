package com.yego.backend.config;

import com.yego.backend.repository.yego_ticketerera.DispositivoRepository;
import com.yego.backend.service.yego_principal.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtRequestFilterTest {

    private final JwtRequestFilter filter = new JwtRequestFilter(
            mock(AuthService.class),
            mock(DispositivoRepository.class),
            mock(JwtTokenProvider.class));

    @Test
    void omiteRefreshPrincipalParaQueLoValideUnaSolaCapa() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void omiteRefreshDeTicketeraParaQueLoValideUnaSolaCapa() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/ticketera/auth/refresh");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void mantieneFiltroEnRutasProtegidasYLogout() {
        MockHttpServletRequest protectedRequest = new MockHttpServletRequest("GET", "/api/auth/profile");
        MockHttpServletRequest logoutRequest = new MockHttpServletRequest("POST", "/api/auth/logout");

        assertThat(filter.shouldNotFilter(protectedRequest)).isFalse();
        assertThat(filter.shouldNotFilter(logoutRequest)).isFalse();
    }
}
