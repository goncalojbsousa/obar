package com.obar.bll.admin;

import com.obar.model.TaxRate;

import java.math.BigDecimal;

/**
 * Read model for IVA tax rates on admin screens.
 */
public final class AdminTaxRateDTO {

    private final Integer id;
    private final String name;
    private final BigDecimal rate;
    private final String description;
    private final Boolean active;

    private AdminTaxRateDTO(Integer id, String name, BigDecimal rate, String description, Boolean active) {
        this.id = id;
        this.name = name;
        this.rate = rate;
        this.description = description;
        this.active = active;
    }

    public static AdminTaxRateDTO from(TaxRate taxRate) {
        if (taxRate == null) {
            throw new IllegalArgumentException("Tax rate must not be null.");
        }

        return new AdminTaxRateDTO(
                taxRate.getId(),
                taxRate.getName(),
                taxRate.getRate(),
                taxRate.getDescription(),
                taxRate.getActive());
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getActive() {
        return active;
    }
}
