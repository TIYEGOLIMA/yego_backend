package com.yego.backend.repository.yego_principal;

import com.yego.backend.entity.yego_principal.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para usuarios del sistema YEGO Principal.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // --- Búsqueda por identificación ---

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username OR u.email = :email")
    boolean existsByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    // --- Listados con rol (evitan N+1) ---

    List<User> findByActive(Boolean active);

    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.role WHERE u.active = :active ORDER BY u.name ASC")
    List<User> findByActiveWithRole(@Param("active") Boolean active);

    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.role ORDER BY u.name ASC")
    List<User> findAllWithRole();

    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.role WHERE u.username LIKE :searchPattern OR u.email LIKE :searchPattern OR u.name LIKE :searchPattern OR u.lastName LIKE :searchPattern ORDER BY u.name ASC")
    List<User> findBySearchWithRole(@Param("searchPattern") String searchPattern);

    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.role WHERE (u.username LIKE :searchPattern OR u.email LIKE :searchPattern OR u.name LIKE :searchPattern OR u.lastName LIKE :searchPattern) AND u.active = :active ORDER BY u.name ASC")
    List<User> findBySearchAndActiveWithRole(@Param("searchPattern") String searchPattern, @Param("active") Boolean active);

    // --- Por ID y roles ---

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.id = :id")
    Optional<User> findByIdWithRole(@Param("id") Long id);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.role.id = :roleId")
    List<User> findByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.role.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
    Long countByRoleName(@Param("roleName") String roleName);

    /** Conteo de usuarios por nombre de rol en una sola consulta (evita N+1 en listado de roles). */
    @Query("SELECT u.role.name, COUNT(u) FROM User u GROUP BY u.role.name")
    List<Object[]> countUsersGroupByRoleName();

    // --- Áreas ---

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.areaId = :areaId ORDER BY u.name ASC")
    List<User> findByAreaId(@Param("areaId") Long areaId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.areaId = :areaId")
    Long countByAreaId(@Param("areaId") Long areaId);

    @Query("SELECT u.areaId, COUNT(u) FROM User u WHERE u.areaId IN :areaIds GROUP BY u.areaId")
    List<Object[]> countByAreaIdIn(@Param("areaIds") List<Long> areaIds);

    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.role WHERE u.id IN :ids")
    List<User> findByIdInWithRole(@Param("ids") List<Long> ids);

    /** Una sola consulta: usuarios activos sin área, excluyendo quienes ya son responsables; incluye opcionalmente un managerId (ej. responsable del área en edición). */
    @Query("SELECT u.id, u.name, u.lastName FROM User u WHERE u.active = true AND u.areaId IS NULL "
           + "AND (u.id NOT IN (SELECT a.managerId FROM Area a WHERE a.managerId IS NOT NULL) "
           + "OR (:includeManagerId IS NOT NULL AND u.id = :includeManagerId)) ORDER BY u.name ASC")
    List<Object[]> findActiveUsersForResponsableDropdown(@Param("includeManagerId") Long includeManagerId);

    /** Proyección para colaboradores de un área (evita cargar entidades completas). */
    @Query("SELECT u.id, u.name, u.lastName, u.email, r.name FROM User u JOIN u.role r WHERE u.areaId = :areaId ORDER BY u.name ASC")
    List<Object[]> findColaboradoresProjectionByAreaId(@Param("areaId") Long areaId);

    // --- Reportes y estadísticas ---

    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    Long countActiveUsers();

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findAllByOrderByCreatedAtDesc();

    Long countByCreatedAtAfter(LocalDateTime date);

    // --- Actualizaciones ---

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("lastLogin") LocalDateTime lastLogin);

    @Modifying
    @Query("UPDATE User u SET u.password = :password, u.passwordChangedAt = :passwordChangedAt WHERE u.id = :userId")
    void updatePassword(@Param("userId") Long userId, @Param("password") String password, @Param("passwordChangedAt") LocalDateTime passwordChangedAt);

    /** IDs de usuarios que coinciden con búsqueda (username, email, nombre completo). */
    @Query("SELECT u.id FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(CONCAT(COALESCE(u.name,''), ' ', COALESCE(u.lastName,''))) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Long> findUserIdsBySearch(@Param("q") String q);
}
