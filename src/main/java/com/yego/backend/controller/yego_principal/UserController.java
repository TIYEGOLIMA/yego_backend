package com.yego.backend.controller.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import com.yego.backend.service.yego_principal.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        return ResponseEntity.ok(userService.findOne(userId));
    }

    @GetMapping
    public ResponseEntity<?> findAll(@RequestParam(required = false) Integer page,
                                     @RequestParam(required = false) Integer limit,
                                     @RequestParam(required = false) String search,
                                     @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(userService.findAll(page, limit, search, active));
    }

    /** Debe ir antes de /{id} para no capturar "listado" como id. */
    @GetMapping("/listado")
    public ResponseEntity<List<UsuarioResumenDto>> findAllResumen() {
        return ResponseEntity.ok(userService.findAllResumen());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findOne(id));
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody CreateUserDto createUserDto) {
        return ResponseEntity.status(201).body(userService.create(createUserDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody UpdateUserDto updateUserDto) {
        return userService.update(id, updateUserDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        userService.remove(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                           @Valid @RequestBody CambiarEstadoDto cambiarEstadoDto) {
        return ResponseEntity.ok(userService.cambiarEstado(id, cambiarEstadoDto.getActivo()));
    }

    @PatchMapping("/{id}/area")
    public ResponseEntity<?> updateArea(@PathVariable Long id, @RequestBody UpdateUserAreaDto dto) {
        Long areaId = dto != null ? dto.getAreaId() : null;
        return ResponseEntity.ok(userService.updateArea(id, areaId));
    }

    @PatchMapping("/{id}/sede")
    public ResponseEntity<?> updateSede(@PathVariable Long id, @RequestBody UpdateUserSedeDto dto) {
        Long sedeId = dto != null ? dto.getSedeId() : null;
        return ResponseEntity.ok(userService.updateSede(id, sedeId));
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<DniResponseDto> consultarDni(@PathVariable String dni) {
        return userService.consultarDni(dni);
    }
}
