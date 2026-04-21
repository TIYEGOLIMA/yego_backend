package com.yego.backend.service.yego_ticketerera.impl;

import com.yego.backend.entity.yego_ticketerera.entities.Option;
import com.yego.backend.repository.yego_ticketerera.OptionRepository;
import com.yego.backend.service.yego_ticketerera.OptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OptionServiceImpl implements OptionService {

    private final OptionRepository optionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Option> obtenerModulosActivos() {
        return optionRepository.findByParentIdIsNullAndActiveTrueOrderByPriorityAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Option> obtenerSubopciones(Long parentId) {
        return optionRepository.findByParentIdAndActiveTrueOrderByPriorityAsc(parentId);
    }
}
