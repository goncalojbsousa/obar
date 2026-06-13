package com.obar;

import com.obar.bll.admin.AdminUserDTO;
import com.obar.bll.admin.AdminVehicleDTO;
import com.obar.bll.TaxRateService;
import com.obar.model.User;
import com.obar.model.Vehicle;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;
import junit.framework.TestCase;

import java.math.BigDecimal;

public class AppTest extends TestCase {

    public void testAdminUserDtoCopiesPresentationData() {
        User user = new User();
        user.setId(7);
        user.setName("Ana Silva");
        user.setEmail("ana@example.com");
        user.setType(UserType.DRIVER);
        user.setStatus(AccountStatus.PENDING);
        user.setPasswordHash("not-exposed");

        AdminUserDTO dto = AdminUserDTO.from(user);

        assertEquals(Integer.valueOf(7), dto.id());
        assertEquals("Ana Silva", dto.name());
        assertEquals(UserType.DRIVER, dto.type());
        assertEquals(AccountStatus.PENDING, dto.status());
    }

    public void testVehicleDtoBuildsDisplayName() {
        Vehicle vehicle = new Vehicle();
        vehicle.setBrand("Toyota");
        vehicle.setModel("Corolla");

        assertEquals("Toyota Corolla", AdminVehicleDTO.from(vehicle).vehicleName());
    }

    public void testPublicInstitutionNifUsesZeroVat() {
        assertEquals(new BigDecimal("0.0000"), TaxRateService.rateForTaxNumber("600000000"));
    }

    public void testOtherNifUsesStandardVat() {
        assertEquals(new BigDecimal("0.2300"), TaxRateService.rateForTaxNumber("500000000"));
        assertEquals(new BigDecimal("0.2300"), TaxRateService.rateForTaxNumber(null));
    }

    public void testFareQuoteAddsVatToNetPrice() {
        TaxRateService service = new TaxRateService();

        TaxRateService.FareQuote quote = service.quoteAtRate(
                new BigDecimal("10.00"),
                new BigDecimal("0.2300"));

        assertEquals(new BigDecimal("2.30"), quote.taxAmount());
        assertEquals(new BigDecimal("12.30"), quote.totalAmount());
    }
}
