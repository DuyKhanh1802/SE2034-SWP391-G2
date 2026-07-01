package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.utils;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentType;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class PaymentCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int RANDOM_LENGTH = 6;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private PaymentCodeGenerator() {
    }

    public static String generate(PaymentType paymentType) {
        String prefix = getPrefix(paymentType);
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String randomPart = generateRandomCode();

        return prefix + "-" + datePart + "-" + randomPart;
    }

    private static String getPrefix(PaymentType paymentType) {
        return switch (paymentType) {
            case DEPOSIT -> "ĐC";
            case BALANCE -> "CL";
            case FULL -> "TT";
            case REFUND -> "HT";
            case INCIDENTAL -> "PS";
        };
    }

    private static String generateRandomCode() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < RANDOM_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }

        return code.toString();
    }
}