package com.yego.backend.entity.yego_gantt.entities.enums;

public enum SprintStatus {
    PLANNED,
    ACTIVE,
    COMPLETED,
    CANCELLED;

    /** Aún se pueden crear o reasignar tareas (no cerrado ni cancelado). */
    public boolean isOpenForAssignment() {
        return this != COMPLETED && this != CANCELLED;
    }
}
