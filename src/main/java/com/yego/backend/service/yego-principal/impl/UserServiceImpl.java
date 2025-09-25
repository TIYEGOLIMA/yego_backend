package com.yego.backend.service.yego_principal.impl;

import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.entity.yego_principal.entities.User;
import com.yego.backend.entity.yego_principal.api.*;
import com.yego.backend.repository.yego_principal.UserRepository;
import com.yego.backend.service.yego_principal.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de usuarios del sistema YEGO Principal
 * Equivalente a UsersService de NestJS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public UserResponseDto create(CreateUserDto createUserDto) {
        // Verificar si el usuario ya existe
        if (userRepository.existsByUsernameOrEmail(createUserDto.getUsername(), createUserDto.getEmail())) {
            throw new IllegalStateException("El usuario o email ya existe");
        }
        
        // Validar contraseña
        if (!validatePassword(createUserDto.getPassword())) {
            throw new IllegalArgumentException("La contraseña no cumple con los requisitos de seguridad");
        }
        
        // Crear usuario
        User user = User.builder()
                .username(createUserDto.getUsername())
                .email(createUserDto.getEmail())
                .name(createUserDto.getName() != null ? createUserDto.getName() : createUserDto.getUsername())
                .password(passwordEncoder.encode(createUserDto.getPassword()))
                .role(createUserDto.getRole() != null ? createUserDto.getRole() : "usuario")
                .moduleId(createUserDto.getModuleId())
                .active(true)
                .build();
        
        User savedUser = userRepository.save(user);
        
        log.info("✅ Usuario YEGO Principal creado: {}", savedUser.getUsername());
        
        return mapToResponseDto(savedUser);
    }
    
    @Override
    @Transactional
    public UserResponseDto createFromExample(CreateUserExampleDto createUserDto) {
        // Verificar si el usuario ya existe
        if (userRepository.existsByUsernameOrEmail(createUserDto.getUsername(), createUserDto.getEmail())) {
            throw new IllegalStateException("El usuario o email ya existe");
        }
        
        // Validar contraseña
        if (!validatePassword(createUserDto.getPassword())) {
            throw new IllegalArgumentException("La contraseña no cumple con los requisitos de seguridad");
        }
        
        // Crear nombre completo desde firstName y lastName
        String fullName = createUserDto.getFirstName() + " " + createUserDto.getLastName();
        
        // Crear usuario
        User user = User.builder()
                .username(createUserDto.getUsername())
                .email(createUserDto.getEmail())
                .name(fullName)
                .password(passwordEncoder.encode(createUserDto.getPassword()))
                .role("usuario") // Rol por defecto
                .active(true)
                .build();
        
        User savedUser = userRepository.save(user);
        
        log.info("✅ Usuario ejemplo YEGO Principal creado: {} ({})", savedUser.getUsername(), fullName);
        
        return mapToResponseDto(savedUser);
    }
    
    @Override
    public UserPageDto findAll(Integer page, Integer limit, String search, Boolean active) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        
        Page<User> userPage;
        
        if (search != null && !search.trim().isEmpty()) {
            if (active != null) {
                userPage = userRepository.findBySearchAndActive(search, active, pageable);
            } else {
                userPage = userRepository.findBySearch(search, pageable);
            }
        } else {
            if (active != null) {
                userPage = userRepository.findByActive(active, pageable);
            } else {
                userPage = userRepository.findAll(pageable);
            }
        }
        
        List<UserResponseCompleteDto> users = userPage.getContent().stream()
                .map(this::mapToUserResponseCompleteDto)
                .collect(Collectors.toList());
        
        log.info("📋 Usuarios YEGO Principal obtenidos: {} de {} total", users.size(), userPage.getTotalElements());
        
        return UserPageDto.builder()
                .users(users)
                .total(userPage.getTotalElements())
                .page(page)
                .limit(limit)
                .search(search)
                .active(active)
                .build();
    }
    
    @Override
    public UserResponseDto findOne(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        return mapToResponseDto(user);
    }
    
    @Override
    public UserResponseDto findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        return mapToResponseDto(user);
    }
    
    @Override
    @Transactional
    public UserResponseDto update(Long id, UpdateUserDto updateUserDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        // Verificar conflictos de username
        if (updateUserDto.getUsername() != null && !updateUserDto.getUsername().equals(user.getUsername())) {
            if (userRepository.findByUsername(updateUserDto.getUsername()).isPresent()) {
                throw new IllegalStateException("El nombre de usuario ya existe");
            }
        }
        
        // Verificar conflictos de email
        if (updateUserDto.getEmail() != null && !updateUserDto.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(updateUserDto.getEmail()).isPresent()) {
                throw new IllegalStateException("El email ya existe");
            }
        }
        
        // Actualizar campos
        if (updateUserDto.getUsername() != null) {
            user.setUsername(updateUserDto.getUsername());
        }
        if (updateUserDto.getEmail() != null) {
            user.setEmail(updateUserDto.getEmail());
        }
        if (updateUserDto.getName() != null) {
            user.setName(updateUserDto.getName());
        }
        if (updateUserDto.getRole() != null) {
            user.setRole(updateUserDto.getRole());
        }
        if (updateUserDto.getModuleId() != null) {
            user.setModuleId(updateUserDto.getModuleId());
        }
        
        // Hash password si se proporciona
        if (updateUserDto.getPassword() != null) {
            if (!validatePassword(updateUserDto.getPassword())) {
                throw new IllegalArgumentException("La contraseña no cumple con los requisitos de seguridad");
            }
            user.setPassword(passwordEncoder.encode(updateUserDto.getPassword()));
        }
        
        User savedUser = userRepository.save(user);
        
        log.info("✅ Usuario YEGO Principal actualizado: {}", savedUser.getUsername());
        
        return mapToResponseDto(savedUser);
    }
    
    @Override
    @Transactional
    public void remove(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        // Soft delete - marcar como inactivo
        user.setActive(false);
        userRepository.save(user);
        
        log.info("🗑️ Usuario YEGO Principal eliminado (soft delete): {}", user.getUsername());
    }
    
    @Override
    @Transactional
    public UserResponseDto activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        user.setActive(true);
        User savedUser = userRepository.save(user);
        
        log.info("✅ Usuario YEGO Principal activado: {}", savedUser.getUsername());
        
        return mapToResponseDto(savedUser);
    }
    
    @Override
    @Transactional
    public UserResponseDto deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        user.setActive(false);
        User savedUser = userRepository.save(user);
        
        log.info("❌ Usuario YEGO Principal desactivado: {}", savedUser.getUsername());
        
        return mapToResponseDto(savedUser);
    }
    
    @Override
    @Transactional
    public void changePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        if (!validatePassword(newPassword)) {
            throw new IllegalArgumentException("La contraseña no cumple con los requisitos de seguridad");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("🔑 Contraseña cambiada para usuario YEGO Principal: {}", user.getUsername());
    }
    
    @Override
    public boolean validatePassword(String password) {
        // Verificar contraseñas débiles
        List<String> weakPasswords = Arrays.asList("123456", "admin", "password", "123456789", "qwerty");
        if (weakPasswords.contains(password.toLowerCase())) {
            return false;
        }
        
        // Verificar requisitos mínimos
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasNumbers = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
        boolean isLongEnough = password.length() >= 8;
        
        return hasUpperCase && hasLowerCase && hasNumbers && hasSpecialChar && isLongEnough;
    }
    
    private UserResponseDto mapToResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .moduleId(user.getModuleId())
                .active(user.getActive())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    private UserResponseCompleteDto mapToUserResponseCompleteDto(User user) {
        return UserResponseCompleteDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .moduleId(user.getModuleId())
                .build();
    }
}
