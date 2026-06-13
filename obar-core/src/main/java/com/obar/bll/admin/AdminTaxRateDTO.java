package com.obar.bll.admin;

import com.obar.model.TaxRate;

import java.math.BigDecimal;

public record AdminTaxRateDTO(
        Integer id,
        String name,
        BigDecimal rate,
        String description,
        Boolean active) {

    public static AdminTaxRateDTO from(TaxRate taxRate) {
        return new AdminTaxRateDTO(
                taxRate.getId(),
                taxRate.getName(),
                taxRate.getRate(),
                taxRate.getDescription(),
                taxRate.getActive());
    }
}
