package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.*;
import com.yego.backend.entity.yego_principal.api.response.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface UserService {

    UserResponseDto create(CreateUserDto createUserDto);

    Object findAll(Integer page, Integer limit, String search, Boolean active);

    List<UsuarioResumenDto> findAllResumen();

    UserResponseDto findOne(Long id);

    UserResponseDto findByUsername(String username);

    ResponseEntity<?> update(Long id, UpdateUserDto updateUserDto);

    UserResponseDto updateArea(Long id, Long areaId);

    UserResponseDto updateSede(Long id, Long sedeId);

    void remove(Long id);

    UserResponseDto cambiarEstado(Long id, Boolean activo);

    void changePassword(Long id, String newPassword);

    boolean validatePassword(String password);

    ResponseEntity<DniResponseDto> consultarDni(String dni);
}
