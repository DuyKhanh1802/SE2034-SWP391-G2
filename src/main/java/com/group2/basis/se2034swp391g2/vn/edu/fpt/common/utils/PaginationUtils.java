package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils;

import java.util.List;
import java.util.stream.IntStream;

public final class PaginationUtils {

    private static final int MAX_VISIBLE_PAGES = 5;

    private PaginationUtils() {
    }

    public static List<Integer> buildVisiblePages(int currentPage, int totalPages) {
        if (totalPages <= 0) {
            return List.of();
        }

        if (totalPages <= MAX_VISIBLE_PAGES) {
            return IntStream.range(0, totalPages).boxed().toList();
        }

        int start;
        int end;
        if (currentPage <= 2) {
            start = 0;
            end = 3;
        } else if (currentPage >= totalPages - 3) {
            start = totalPages - 4;
            end = totalPages - 1;
        } else {
            start = currentPage - 1;
            end = currentPage + 2;
        }

        return IntStream.rangeClosed(start, end).boxed().toList();
    }
}
