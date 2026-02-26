package com.yego.backend.exception;

/**
 * Se lanza cuando un usuario sin permiso (no jefe de área ni administrador) intenta exportar el listado de asistencias.
 */
public class ExportacionNoPermitidaException extends RuntimeException {

    public ExportacionNoPermitidaException() {
        super("Solo los jefes de área y los administradores pueden exportar el listado de asistencias.");
    }
}
