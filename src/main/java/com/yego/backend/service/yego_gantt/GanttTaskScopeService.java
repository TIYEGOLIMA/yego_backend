package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_principal.entities.User;

/** Resuelve el ámbito de áreas ({@link GanttTaskScope}) para operaciones sobre tareas. */
public interface GanttTaskScopeService {

    GanttTaskScope resolve(User user);
}
