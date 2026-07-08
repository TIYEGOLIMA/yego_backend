package com.yego.backend.controller.yego_pro_ops;

import com.yego.backend.entity.yego_pro_ops.api.request.AgregarFlotaRequest;
import com.yego.backend.entity.yego_pro_ops.api.response.FleetSegmentResponse;
import com.yego.backend.service.yego_pro_ops.FleetSegmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/vehicles/segments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FleetSegmentController {

    private final FleetSegmentService fleetSegmentService;

    @GetMapping
    public List<FleetSegmentResponse> listar() {
        return fleetSegmentService.listarFlotas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FleetSegmentResponse agregar(@RequestBody AgregarFlotaRequest request, Authentication authentication) {
        Long createdById = resolverUsuarioId(authentication);
        return fleetSegmentService.agregarFlota(request.getParkId(), createdById);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        fleetSegmentService.desactivarFlota(id);
    }

    private Long resolverUsuarioId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) return null;
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
