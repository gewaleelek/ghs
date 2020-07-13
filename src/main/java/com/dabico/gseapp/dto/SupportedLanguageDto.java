package com.dabico.gseapp.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SupportedLanguageDto {
    Long id;
    String language;
}
