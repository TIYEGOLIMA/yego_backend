package com.yego.backend.service.yego_ticketerera;

import com.yego.backend.entity.yego_ticketerera.entities.Option;

import java.util.List;

public interface OptionService {

    List<Option> obtenerModulosActivos();

    List<Option> obtenerSubopciones(Long parentId);
}
