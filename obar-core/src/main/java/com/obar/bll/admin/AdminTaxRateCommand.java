package com.obar.bll.admin;

import java.math.BigDecimal;

/**
 * Command payload to update IVA tax rates.
 */
public record AdminTaxRateCommand(
        String name,
        BigDecimal rate,
        String description,
        Boolean active) {
}
