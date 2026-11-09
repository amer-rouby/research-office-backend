package com.researchoffice.catalog.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public contact details shown on the website (changed by environment variables, no rebuild). */
@RestController
class OfficeInfoController {

    private final OfficeInfo info;

    OfficeInfoController(OfficeProperties props) {
        this.info = new OfficeInfo(props.name(), blankToNull(props.whatsapp()), blankToNull(props.email()));
    }

    @GetMapping("/api/public/office")
    OfficeInfo office() {
        return info;
    }

    record OfficeInfo(String name,
                      @Schema(nullable = true, description = "International number, digits only, e.g. 97455551234") String whatsapp,
                      @Schema(nullable = true) String email) {
    }

    @ConfigurationProperties("ro.office")
    record OfficeProperties(String name, String whatsapp, String email) {
        OfficeProperties {
            name = name == null || name.isBlank() ? "مكتب الخدمات البحثية" : name.trim();
            whatsapp = whatsapp == null ? null : whatsapp.replaceAll("\\D", "");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
