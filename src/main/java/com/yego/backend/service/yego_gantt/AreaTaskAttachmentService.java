package com.yego.backend.service.yego_gantt;

import com.yego.backend.entity.yego_gantt.api.response.AreaTaskAttachmentResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AreaTaskAttachmentService {

    List<AreaTaskAttachmentResponseDto> list(Long userId, Long taskId);

    AreaTaskAttachmentResponseDto upload(Long userId, Long taskId, MultipartFile file);

    void delete(Long userId, Long taskId, Long attachmentId);
}
