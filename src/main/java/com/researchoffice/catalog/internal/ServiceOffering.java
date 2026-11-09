package com.researchoffice.catalog.internal;

import com.researchoffice.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "service_offering")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class ServiceOffering extends BaseEntity {

    enum Category { ACADEMIC, BUSINESS }

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private Category category;

    @Column(name = "name_ar", nullable = false)
    private String nameAr;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "description_ar", nullable = false)
    private String descriptionAr;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    void update(String nameAr, String nameEn, String descriptionAr, boolean active, int sortOrder) {
        this.nameAr = nameAr.trim();
        this.nameEn = nameEn.trim();
        this.descriptionAr = descriptionAr.trim();
        this.active = active;
        this.sortOrder = sortOrder;
    }
}
