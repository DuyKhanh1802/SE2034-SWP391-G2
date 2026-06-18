package com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums;

public enum IdentityType {
    CCCD("Căn cước công dân"),
    CMND("Chứng minh nhân dân" ),
    PASSPORT("Hộ chiếu"),
    OTHER("Khác");

    private final String label;

    IdentityType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
