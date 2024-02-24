package com.zzy.shortlink.project.common.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VailDateTypeEnum {
    PERMANENT(0),
    CUSTOM(1);

    private final int type;
}
