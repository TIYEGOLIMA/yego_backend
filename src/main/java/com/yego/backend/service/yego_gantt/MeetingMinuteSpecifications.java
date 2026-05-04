package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.entities.WorkosMeetingMinute;
import com.yego.backend.entity.yego_gantt.entities.WorkosMeetingMinuteItem;
import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteStatus;
import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Set;

public final class MeetingMinuteSpecifications {

    private MeetingMinuteSpecifications() {
    }

    public static Specification<WorkosMeetingMinute> notDeleted() {
        return (root, q, cb) -> cb.isFalse(root.get("deleted"));
    }

    /**
     * Visibilidad WorkOS: admin ve todo; resto ve actas donde es creador/dueño o tiene ítem como responsable
     * o con área en su ámbito.
     */
    public static Specification<WorkosMeetingMinute> visibleTo(GanttTaskScope scope, long userId, boolean platformAdmin) {
        return (root, q, cb) -> {
            Predicate notDel = cb.isFalse(root.get("deleted"));
            if (platformAdmin) {
                return notDel;
            }
            Predicate own = cb.or(
                    cb.equal(root.get("createdByUserId"), userId),
                    cb.equal(root.get("ownerUserId"), userId));
            Set<Long> aids = scope.areaIds();
            Subquery<Long> sq = q.subquery(Long.class);
            Root<WorkosMeetingMinuteItem> i = sq.from(WorkosMeetingMinuteItem.class);
            sq.select(i.get("id"));
            Predicate itemMatch;
            if (aids == null || aids.isEmpty()) {
                itemMatch = cb.equal(i.get("responsibleUserId"), userId);
            } else {
                itemMatch = cb.or(
                        cb.equal(i.get("responsibleUserId"), userId),
                        i.get("areaId").in(aids));
            }
            sq.where(cb.and(cb.equal(i.get("meetingMinute"), root), itemMatch));
            return cb.and(notDel, cb.or(own, cb.exists(sq)));
        };
    }

    public static Specification<WorkosMeetingMinute> statusOptional(MeetingMinuteStatus status) {
        return (root, q, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<WorkosMeetingMinute> meetingTypeOptional(MeetingMinuteType type) {
        return (root, q, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("meetingType"), type);
    }

    public static Specification<WorkosMeetingMinute> meetingDateFrom(LocalDate from) {
        return (root, q, cb) -> from == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("meetingDate"), from);
    }

    public static Specification<WorkosMeetingMinute> meetingDateTo(LocalDate to) {
        return (root, q, cb) -> to == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("meetingDate"), to);
    }

    public static Specification<WorkosMeetingMinute> ownerOptional(Long ownerUserId) {
        return (root, q, cb) -> ownerUserId == null ? cb.conjunction() : cb.equal(root.get("ownerUserId"), ownerUserId);
    }

    public static Specification<WorkosMeetingMinute> hasItemWithProject(Long projectId) {
        if (projectId == null) {
            return (root, q, cb) -> cb.conjunction();
        }
        return (root, q, cb) -> {
            Subquery<Long> sq = q.subquery(Long.class);
            Root<WorkosMeetingMinuteItem> i = sq.from(WorkosMeetingMinuteItem.class);
            sq.select(i.get("id"));
            sq.where(cb.and(
                    cb.equal(i.get("meetingMinute"), root),
                    cb.equal(i.get("projectId"), projectId)));
            return cb.exists(sq);
        };
    }

    public static Specification<WorkosMeetingMinute> hasItemWithArea(Long areaId) {
        if (areaId == null) {
            return (root, q, cb) -> cb.conjunction();
        }
        return (root, q, cb) -> {
            Subquery<Long> sq = q.subquery(Long.class);
            Root<WorkosMeetingMinuteItem> i = sq.from(WorkosMeetingMinuteItem.class);
            sq.select(i.get("id"));
            sq.where(cb.and(
                    cb.equal(i.get("meetingMinute"), root),
                    cb.equal(i.get("areaId"), areaId)));
            return cb.exists(sq);
        };
    }
}

