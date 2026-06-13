package com.obar.bll;

import com.obar.dal.TaxRateRepository;
import com.obar.model.TaxRate;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TaxRateService {

    public static final BigDecimal EXEMPT_RATE = new BigDecimal("0.0000");
    public static final BigDecimal STANDARD_RATE = new BigDecimal("0.2300");

    private final TaxRateRepository taxRateRepository;

    public TaxRateService() {
        this(new TaxRateRepository());
    }

    TaxRateService(TaxRateRepository taxRateRepository) {
        this.taxRateRepository = taxRateRepository;
    }

    public FareQuote quote(BigDecimal netAmount, String taxNumber) {
        return quoteAtRate(netAmount, resolveRate(taxNumber));
    }

    public FareQuote quoteAtRate(BigDecimal netAmount, BigDecimal rate) {
        if (netAmount == null || netAmount.signum() < 0) {
            throw new IllegalArgumentException("O valor base da viagem e invalido.");
        }
        if (rate == null || rate.signum() < 0) {
            throw new IllegalArgumentException("A taxa de IVA da viagem e invalida.");
        }

        BigDecimal normalizedNet = netAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = normalizedNet.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return new FareQuote(normalizedNet, rate, taxAmount, normalizedNet.add(taxAmount));
    }

    public BigDecimal resolveRate(String taxNumber) {
        BigDecimal requiredRate = rateForTaxNumber(taxNumber);
        TaxRate taxRate = taxRateRepository.findActiveByRate(requiredRate)
                .orElseThrow(() -> new IllegalStateException(
                        "A taxa de IVA de " + requiredRate.movePointRight(2).stripTrailingZeros().toPlainString()
                                + "% nao esta ativa."));
        return taxRate.getRate();
    }

    public static BigDecimal rateForTaxNumber(String taxNumber) {
        String normalizedTaxNumber = taxNumber == null ? "" : taxNumber.trim();
        return normalizedTaxNumber.startsWith("6") ? EXEMPT_RATE : STANDARD_RATE;
    }

    public record FareQuote(
            BigDecimal netAmount,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal totalAmount) {
    }
}
