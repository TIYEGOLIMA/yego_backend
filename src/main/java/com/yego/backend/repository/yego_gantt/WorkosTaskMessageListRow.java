package com.yego.backend.repository.yego_gantt;

import java.time.LocalDateTime;

/**
 * Proyección para listados de mensajes (consulta nativa con JOIN a users).
 */
public interface WorkosTaskMessageListRow {

    Long getId();

    Long getTaskId();

    Long getSubtaskId();

    Long getAuthorUserId();

    String getAuthorName();

    String getMessageType();

    String getContent();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();

    Boolean getDeleted();
}
