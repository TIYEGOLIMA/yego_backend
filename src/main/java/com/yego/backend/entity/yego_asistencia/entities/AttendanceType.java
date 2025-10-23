package com.yego.backend.entity.yego_asistencia.entities;

public enum AttendanceType {
    ENTRY("entry"),
    EXIT_BREAK("exit_break"),
    RETURN_BREAK("return_break"),
    EXIT("exit");
    
    private final String value;
    
    AttendanceType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static AttendanceType fromValue(String value) {
        for (AttendanceType type : AttendanceType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de asistencia no válido: " + value);
    }
}
