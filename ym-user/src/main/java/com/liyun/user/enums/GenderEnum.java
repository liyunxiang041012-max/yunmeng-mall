 package com.liyun.user.enums;

import lombok.Getter;

/**
 * 性别枚举
 * <p>0=未知, 1=男, 2=女</p>
 */
@Getter
public enum GenderEnum {

    UNKNOWN(0, "未知"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final int code;
    private final String label;

    GenderEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /** 前端 "male"/"female" → 枚举 */
    public static GenderEnum fromFrontend(String value) {
        if (value == null || value.isBlank()) return UNKNOWN;
        return switch (value.toLowerCase()) {
            case "male" -> MALE;
            case "female" -> FEMALE;
            default -> UNKNOWN;
        };
    }

    /** DB 存值 → 枚举 */
    public static GenderEnum fromCode(Integer code) {
        if (code == null) return UNKNOWN;
        return switch (code) {
            case 1 -> MALE;
            case 2 -> FEMALE;
            default -> UNKNOWN;
        };
    }

    /** 枚举 → 前端展示用的 key（male/female） */
    public String toFrontend() {
        return switch (this) {
            case MALE -> "male";
            case FEMALE -> "female";
            default -> "unknown";
        };
    }
}
