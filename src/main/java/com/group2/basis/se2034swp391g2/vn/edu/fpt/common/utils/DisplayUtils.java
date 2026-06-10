package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.User;

public final class DisplayUtils {

    private DisplayUtils() {
    }

    public static String formatDisplayName(User user) {
        if (user == null) {
            return "Chưa có tên";
        }

        String firstPart = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lastPart = user.getLastName() != null ? user.getLastName().trim() : "";

        if (!firstPart.isEmpty() && !lastPart.isEmpty()) {
            return lastPart + " " + firstPart;
        }
        if (!lastPart.isEmpty()) {
            return lastPart;
        }
        if (!firstPart.isEmpty()) {
            return firstPart;
        }
        return "Chưa có tên";
    }
}
