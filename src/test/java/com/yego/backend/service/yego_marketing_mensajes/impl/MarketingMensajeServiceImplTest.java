package com.yego.backend.service.yego_marketing_mensajes.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yego.backend.entity.yego_marketing_mensajes.api.request.MarketingMensajeRequest;
import com.yego.backend.entity.yego_marketing_mensajes.entities.MarketingMensaje;
import com.yego.backend.repository.yego_marketing_mensajes.MarketingMensajeRepository;
import com.yego.backend.repository.yego_marketing_mensajes.ModuleMarketingGroupRepository;
import com.yego.backend.service.MinIOService;
import com.yego.backend.service.yego_garantizado.FlotaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketingMensajeServiceImplTest {

    @Test
    void rechazaCampanaActivaSinCanal() {
        TestContext context = context();
        MarketingMensajeRequest request = requestBase();

        assertThatThrownBy(() -> context.service.crearMensaje(request, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("al menos un canal");
        verify(context.messageRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaHorarioQueNoEsJsonValido() {
        TestContext context = context();
        MarketingMensajeRequest request = requestBase();
        request.setWhatsapp(false);
        request.setGrupos(List.of("grupo-1"));
        request.setHorasEspecificas("{invalido");

        assertThatThrownBy(() -> context.service.crearMensaje(request, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("JSON válido");
        verify(context.messageRepository, never()).saveAndFlush(any());
    }

    @Test
    void normalizaDestinatariosAntesDePersistir() {
        TestContext context = context();
        MarketingMensajeRequest request = requestBase();
        request.setWhatsapp(false);
        request.setGrupos(List.of(" grupo-1 ", "", "grupo-1", "grupo-2"));
        request.setHorasEspecificas("{\"10:00\":[\"Lun\"]}");
        when(context.messageRepository.saveAndFlush(any(MarketingMensaje.class)))
                .thenAnswer(invocation -> {
                    MarketingMensaje value = invocation.getArgument(0);
                    value.setId(10L);
                    return value;
                });

        context.service.crearMensaje(request, null);

        ArgumentCaptor<MarketingMensaje> captor = ArgumentCaptor.forClass(MarketingMensaje.class);
        verify(context.messageRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getGrupos()).isEqualTo("[\"grupo-1\",\"grupo-2\"]");
        assertThat(captor.getValue().getWhatsapp()).isTrue();
    }

    @Test
    void validaCampanaAntesDeSubirArchivo() {
        TestContext context = context();
        MarketingMensajeRequest request = requestBase();
        MockMultipartFile file = new MockMultipartFile(
                "file", "campana.png", "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> context.service.crearMensaje(request, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("al menos un canal");

        verify(context.minIOService, never()).subirArchivo(any());
        verify(context.messageRepository, never()).saveAndFlush(any());
    }

    private MarketingMensajeRequest requestBase() {
        MarketingMensajeRequest request = new MarketingMensajeRequest();
        request.setTitulo("Campaña");
        request.setMensaje("Mensaje");
        request.setActivo(true);
        request.setWhatsapp(false);
        request.setYandex(false);
        return request;
    }

    private TestContext context() {
        MarketingMensajeRepository messageRepository = mock(MarketingMensajeRepository.class);
        ModuleMarketingGroupRepository groupRepository =
                mock(ModuleMarketingGroupRepository.class);
        FlotaService flotaService = mock(FlotaService.class);
        MinIOService minIOService = mock(MinIOService.class);
        MarketingMensajeServiceImpl service = new MarketingMensajeServiceImpl(
                messageRepository,
                groupRepository,
                flotaService,
                minIOService,
                new ObjectMapper());
        return new TestContext(service, messageRepository, minIOService);
    }

    private record TestContext(
            MarketingMensajeServiceImpl service,
            MarketingMensajeRepository messageRepository,
            MinIOService minIOService) {
    }
}
