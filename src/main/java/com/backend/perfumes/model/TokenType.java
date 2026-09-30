package com.backend.perfumes.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {

    ACCESS("access"),
    REFRESH("refresh");

    private final String value;
}