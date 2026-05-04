package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.request.*;
import com.yego.backend.entity.yego_gantt.api.response.*;
import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteStatus;
import com.yego.backend.entity.yego_gantt.entities.enums.MeetingMinuteType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MeetingMinuteService {

    Page<MeetingMinuteResponse> list(
            long userId,
            MeetingMinuteStatus status,
            MeetingMinuteType meetingType,
            java.time.LocalDate dateFrom,
            java.time.LocalDate dateTo,
            Long ownerUserId,
            Long projectId,
            Long areaId,
            Pageable pageable);

    MeetingMinuteResponse getById(long userId, long minuteId);

    MeetingMinuteResponse create(long userId, CreateMeetingMinuteRequest req);

    MeetingMinuteResponse update(long userId, long minuteId, UpdateMeetingMinuteRequest req);

    void patchStatus(long userId, long minuteId, PatchMeetingMinuteStatusRequest req);

    void softDelete(long userId, long minuteId);

    MeetingMinuteResponse addItems(long userId, long minuteId, java.util.List<CreateMeetingMinuteItemRequest> items);

    MeetingMinuteResponse updateItem(long userId, long minuteId, long itemId, UpdateMeetingMinuteItemRequest req);

    void deleteItem(long userId, long minuteId, long itemId);

    ConvertMeetingItemToTaskResponse convertItemToTask(
            long userId,
            long minuteId,
            long itemId,
            ConvertMeetingItemToTaskRequest req);

    java.util.List<MeetingMinuteItemResponse> listUnconverted(long userId);

    MeetingMinutesDashboardKpisResponse dashboardKpis(long userId);
}
