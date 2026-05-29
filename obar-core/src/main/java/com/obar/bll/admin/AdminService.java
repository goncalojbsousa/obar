package com.obar.bll.admin;

import com.obar.bll.auth.PasswordService;
import com.obar.dal.PaymentRepository;
import com.obar.dal.RouteRepository;
import com.obar.dal.TaxRateRepository;
import com.obar.dal.TripDriverRepository;
import com.obar.dal.TripRepository;
import com.obar.dal.UserRepository;
import com.obar.dal.VehicleRepository;
import com.obar.model.enums.UserType;

import java.util.List;

/**
 * Public BLL facade for all admin desktop use cases.
 *
 * <p>
 * The facade keeps the JavaFX layer simple while the actual admin logic is
 * split into smaller services by responsibility.
 * </p>
 */
public class AdminService {

    private final AdminUserManagementService userManagementService;
    private final AdminTripManagementService tripManagementService;
    private final AdminFinancialService financialService;

    public AdminService() {
        this(
                new UserRepository(),
                new PasswordService(),
                new TripRepository(),
                new TripDriverRepository(),
                new RouteRepository(),
                new VehicleRepository(),
                new PaymentRepository(),
                new TaxRateRepository());
    }

    public AdminService(UserRepository userRepository, PasswordService passwordService) {
        this(
                userRepository,
                passwordService,
                new TripRepository(),
                new TripDriverRepository(),
                new RouteRepository(),
                new VehicleRepository(),
                new PaymentRepository(),
                new TaxRateRepository());
    }

    public AdminService(
            UserRepository userRepository,
            PasswordService passwordService,
            TripRepository tripRepository,
            TripDriverRepository tripDriverRepository,
            RouteRepository routeRepository,
            VehicleRepository vehicleRepository) {
        this(
                userRepository,
                passwordService,
                tripRepository,
                tripDriverRepository,
                routeRepository,
                vehicleRepository,
                new PaymentRepository(),
                new TaxRateRepository());
    }

    public AdminService(
            UserRepository userRepository,
            PasswordService passwordService,
            TripRepository tripRepository,
            TripDriverRepository tripDriverRepository,
            RouteRepository routeRepository,
            VehicleRepository vehicleRepository,
            PaymentRepository paymentRepository,
            TaxRateRepository taxRateRepository) {
        this.userManagementService = new AdminUserManagementService(userRepository, passwordService);
        this.tripManagementService = new AdminTripManagementService(
                userRepository,
                tripRepository,
                tripDriverRepository,
                routeRepository,
                vehicleRepository);
        this.financialService = new AdminFinancialService(paymentRepository, taxRateRepository);
    }

    public List<AdminUserDTO> listPendingDrivers() {
        return userManagementService.listPendingDrivers();
    }

    public List<AdminUserDTO> listUsersByType(UserType type) {
        return userManagementService.listUsersByType(type);
    }

    public AdminUserDTO createUser(AdminUserCommand command) {
        return userManagementService.createUser(command);
    }

    public AdminUserDTO updateUser(Integer userId, AdminUserCommand command) {
        return userManagementService.updateUser(userId, command);
    }

    public void approveDriver(Integer userId, String approvalNote) {
        userManagementService.approveDriver(userId, approvalNote);
    }

    public void rejectDriver(Integer userId, String rejectionNote) {
        userManagementService.rejectDriver(userId, rejectionNote);
    }

    public boolean blockOrDeleteUser(Integer userId) {
        return userManagementService.blockOrDeleteUser(userId);
    }

    public List<AdminTripDTO> listTrips() {
        return tripManagementService.listTrips();
    }

    public AdminTripDTO createTrip(AdminTripCommand command) {
        return tripManagementService.createTrip(command);
    }

    public AdminTripDTO updateTrip(Integer tripId, AdminTripCommand command) {
        return tripManagementService.updateTrip(tripId, command);
    }

    public void deleteTrip(Integer tripId) {
        tripManagementService.deleteTrip(tripId);
    }

    public AdminFinancialOverviewDTO getFinancialOverview(AdminFinancialPeriod period) {
        return financialService.getFinancialOverview(period);
    }

    public List<AdminPaymentByTripDTO> listPaymentsByTrip(AdminFinancialPeriod period) {
        return financialService.listPaymentsByTrip(period);
    }

    public List<AdminTaxRateDTO> listTaxRates() {
        return financialService.listTaxRates();
    }

    public AdminTaxRateDTO updateTaxRate(Integer taxRateId, AdminTaxRateCommand command) {
        return financialService.updateTaxRate(taxRateId, command);
    }
}
