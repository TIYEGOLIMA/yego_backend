package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.CreateAreaDto;
import com.yego.backend.entity.yego_principal.api.request.UpdateAreaDto;
import com.yego.backend.entity.yego_principal.api.response.AreaResponseDto;
import com.yego.backend.entity.yego_principal.api.response.AreaSimpleDto;
import com.yego.backend.entity.yego_principal.api.response.ColaboradorDto;
import com.yego.backend.entity.yego_principal.api.response.UserSimpleDto;
import com.yego.backend.service.yego_principal.AreaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    @PostMapping("/create")
    public ResponseEntity<AreaResponseDto> create(@Valid @RequestBody CreateAreaDto dto) {
        AreaResponseDto area = areaService.create(dto);
        return ResponseEntity.status(201).body(area);
    }

    @GetMapping("/find-all")
    public ResponseEntity<List<AreaResponseDto>> findAll() {
        return ResponseEntity.ok(areaService.findAll());
    }

    @GetMapping("/find-all-active")
    public ResponseEntity<List<AreaSimpleDto>> findAllActive() {
        return ResponseEntity.ok(areaService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AreaResponseDto> findOne(@PathVariable Long id) {
        return ResponseEntity.ok(areaService.findOne(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<AreaResponseDto> update(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateAreaDto dto) {
        return ResponseEntity.ok(areaService.update(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        areaService.remove(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/toggle-status/{id}")
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id) {
        areaService.toggleStatus(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuarios-para-responsable")
    public ResponseEntity<List<UserSimpleDto>> getUsuariosParaResponsable(
            @RequestParam(required = false) Long areaId) {
        return ResponseEntity.ok(areaService.findUsersForResponsable(areaId));
    }

    @GetMapping("/{id}/colaboradores")
    public ResponseEntity<List<ColaboradorDto>> getColaboradores(@PathVariable Long id) {
        return ResponseEntity.ok(areaService.getColaboradoresByAreaId(id));
    }
}
