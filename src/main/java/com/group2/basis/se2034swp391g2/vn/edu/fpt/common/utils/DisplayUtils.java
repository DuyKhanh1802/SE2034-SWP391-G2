package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class DisplayUtils {

    private static final String DEFAULT_NAME = "Chưa có tên";

    private DisplayUtils() {
    }

    public static String formatDisplayName(User user) {
        if (user == null) {
            return DEFAULT_NAME;
        }

        String displayName = joinParts(user.getLastName(), user.getFirstName());

        return displayName.isEmpty() ? DEFAULT_NAME : displayName;
    }

    private static String joinParts(String... parts) {
        return Arrays.stream(parts)
                .filter(DisplayUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}