package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.AreaTask;
import com.yego.backend.entity.yego_principal.entities.User;

/** Privacidad de tareas marcadas como privadas. */
public interface AreaTaskPrivateAccessService {

    boolean canSeeTaskContent(User viewer, AreaTask task);

    void assertCanMutatePrivateTask(User user, AreaTask task);
}
