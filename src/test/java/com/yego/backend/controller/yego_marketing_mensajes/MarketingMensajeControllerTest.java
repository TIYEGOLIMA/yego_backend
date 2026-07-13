package com.yego.backend.controller.yego_marketing_mensajes;

import com.yego.backend.entity.yego_marketing_mensajes.api.request.MarketingMensajeRequest;
import com.yego.backend.entity.yego_marketing_mensajes.api.response.MarketingMensajeResponse;
import com.yego.backend.exception.GlobalApiExceptionHandler;
import com.yego.backend.service.yego_marketing_mensajes.MarketingMensajeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarketingMensajeControllerTest {

    private MarketingMensajeService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(MarketingMensajeService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MarketingMensajeController(service))
                .setControllerAdvice(new GlobalApiExceptionHandler())
                .build();
    }

    @Test
    void crearDelegaEnServicioYRespondeCreated() throws Exception {
        MarketingMensajeResponse response = new MarketingMensajeResponse();
        response.setId(10L);
        when(service.crearMensaje(any(MarketingMensajeRequest.class), isNull()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/marketing-mensajes")
                        .param("titulo", "Campana")
                        .param("mensaje", "Mensaje de prueba"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));

        verify(service).crearMensaje(any(MarketingMensajeRequest.class), isNull());
    }

    @Test
    void eliminarDelegaEnServicioYRespondeNoContent() throws Exception {
        mockMvc.perform(delete("/api/marketing-mensajes/{id}", 12L))
                .andExpect(status().isNoContent());

        verify(service).eliminarMensaje(12L);
    }

    @Test
    void errorDelServicioLoResuelveElManejadorGlobal() throws Exception {
        when(service.obtenerMensajePorId(99L)).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));

        mockMvc.perform(get("/api/marketing-mensajes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Mensaje no encontrado"));
    }
}
