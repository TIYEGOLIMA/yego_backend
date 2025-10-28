package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para usuarios del sistema YEGO Principal
 * Equivalente a UserRepository de TypeORM
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Buscar usuario por username
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Buscar usuario por email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Verificar si existe usuario por username o email
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username OR u.email = :email")
    boolean existsByUsernameOrEmail(@Param("username") String username, @Param("email") String email);
    
    /**
     * Buscar usuarios por estado activo
     */
    Page<User> findByActive(Boolean active, Pageable pageable);
    
    List<User> findByActive(Boolean active);
    
    /**
     * Buscar usuarios por término de búsqueda
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:search% OR u.email LIKE %:search% OR u.name LIKE %:search%")
    Page<User> findBySearch(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.username LIKE %:search% OR u.email LIKE %:search% OR u.name LIKE %:search%")
    List<User> findBySearch(@Param("search") String search);
    
    /**
     * Buscar usuarios por término de búsqueda y estado activo
     */
    @Query("SELECT u FROM User u WHERE (u.username LIKE %:search% OR u.email LIKE %:search% OR u.name LIKE %:search%) AND u.active = :active")
    Page<User> findBySearchAndActive(@Param("search") String search, @Param("active") Boolean active, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE (u.username LIKE %:search% OR u.email LIKE %:search% OR u.name LIKE %:search%) AND u.active = :active")
    List<User> findBySearchAndActive(@Param("search") String search, @Param("active") Boolean active);
    
    /**
     * Contar usuarios activos
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    Long countActiveUsers();
    
    // NUEVAS queries optimizadas con JOIN FETCH
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.id = :id")
    Optional<User> findByIdWithRole(@Param("id") Long id);
    
    @Query("SELECT u FROM User u JOIN FETCH u.role ORDER BY u.name ASC")
    List<User> findAllWithRoles();
    
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.role.id = :roleId")
    List<User> findByRoleId(@Param("roleId") Long roleId);
    
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.role.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
    
    /**
     * Contar usuarios creados después de una fecha
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt > :date")
    Long countUsersCreatedAfter(@Param("date") LocalDateTime date);
    
    /**
     * Obtener todos los usuarios ordenados por fecha de creación
     */
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    Page<User> findAllOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Buscar usuario por username o email
     */
    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);
    
    /**
     * Actualizar último login
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("lastLogin") LocalDateTime lastLogin);
    
    /**
     * Actualizar contraseña
     */
    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.id = :userId")
    void updatePassword(@Param("userId") Long userId, @Param("password") String password);
    
    /**
     * Obtener todos los usuarios ordenados por fecha de creación (sin paginación)
     */
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findAllByOrderByCreatedAtDesc();
    
    /**
     * Contar usuarios creados después de una fecha
     */
    Long countByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Contar usuarios por nombre de rol
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
    Long countByRoleName(@Param("roleName") String roleName);
    
}

