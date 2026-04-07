package com.yego.backend.entity.yego_api_externo.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YangoSuggestionsBody {

    @Builder.Default
    @JsonProperty("suggestions")
    private List<YangoSuggestItem> suggestions = new ArrayList<>();
}
