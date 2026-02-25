package com.yego.backend.service.yego_principal;

import com.yego.backend.entity.yego_principal.api.request.CreateAreaDto;
import com.yego.backend.entity.yego_principal.api.request.UpdateAreaDto;
import com.yego.backend.entity.yego_principal.api.response.AreaResponseDto;
import com.yego.backend.entity.yego_principal.api.response.AreaSimpleDto;
import com.yego.backend.entity.yego_principal.api.response.ColaboradorDto;
import com.yego.backend.entity.yego_principal.api.response.UserSimpleDto;

import java.util.List;

public interface AreaService {

    AreaResponseDto create(CreateAreaDto dto);

    List<AreaResponseDto> findAll();

    List<AreaSimpleDto> findAllActive();

    AreaResponseDto findOne(Long id);

    AreaResponseDto update(Long id, UpdateAreaDto dto);

    void remove(Long id);

    AreaResponseDto toggleStatus(Long id);

    List<UserSimpleDto> findUsersForResponsable();

    List<ColaboradorDto> getColaboradoresByAreaId(Long areaId);
}
