package com.obar.bll.admin;

import com.obar.bll.auth.PasswordService;
import com.obar.dal.PaymentRepository;
import com.obar.dal.RouteRepository;
import com.obar.dal.TaxRateRepository;
import com.obar.dal.TripRepository;
import com.obar.dal.TripDriverRepository;
import com.obar.dal.UserRepository;
import com.obar.dal.VehicleRepository;
import com.obar.model.Route;
import com.obar.model.TaxRate;
import com.obar.model.Trip;
import com.obar.model.User;
import com.obar.model.Vehicle;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.PaymentStatus;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import com.obar.model.enums.UserType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * BLL service for admin user-management use-cases.
 */
public class AdminService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final TripRepository tripRepository;
    private final TripDriverRepository tripDriverRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final PaymentRepository paymentRepository;
    private final TaxRateRepository taxRateRepository;

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
        if (userRepository == null) {
            throw new IllegalArgumentException("UserRepository must not be null.");
        }
        if (passwordService == null) {
            throw new IllegalArgumentException("PasswordService must not be null.");
        }
        if (tripRepository == null) {
            throw new IllegalArgumentException("TripRepository must not be null.");
        }
        if (tripDriverRepository == null) {
            throw new IllegalArgumentException("TripDriverRepository must not be null.");
        }
        if (routeRepository == null) {
            throw new IllegalArgumentException("RouteRepository must not be null.");
        }
        if (vehicleRepository == null) {
            throw new IllegalArgumentException("VehicleRepository must not be null.");
        }
        if (paymentRepository == null) {
            throw new IllegalArgumentException("PaymentRepository must not be null.");
        }
        if (taxRateRepository == null) {
            throw new IllegalArgumentException("TaxRateRepository must not be null.");
        }
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.tripRepository = tripRepository;
        this.tripDriverRepository = tripDriverRepository;
        this.routeRepository = routeRepository;
        this.vehicleRepository = vehicleRepository;
        this.paymentRepository = paymentRepository;
        this.taxRateRepository = taxRateRepository;
    }

    public List<AdminUserDTO> listPendingDrivers() {
        return userRepository.findPendingDrivers().stream()
                .map(AdminUserDTO::from)
                .toList();
    }

    public List<AdminUserDTO> listUsersByType(UserType type) {
        if (type == null) {
            throw new IllegalArgumentException("User type is required.");
        }
        return userRepository.findByType(type).stream()
                .map(AdminUserDTO::from)
                .toList();
    }

    public List<AdminTripDTO> listTrips() {
        return tripRepository.findAllForAdminDashboard().stream()
                .map(AdminTripDTO::from)
                .toList();
    }

    public AdminFinancialOverviewDTO getFinancialOverview(AdminFinancialPeriod period) {
        AdminFinancialPeriod safePeriod = period == null ? AdminFinancialPeriod.ALL : period;

        LocalDateTime start = resolvePeriodStart(safePeriod);
        LocalDateTime end = resolvePeriodEnd(safePeriod);

        BigDecimal totalIncome = paymentRepository.sumAmountByStatus(PaymentStatus.PROCESSED, null, null);
        BigDecimal periodIncome = paymentRepository.sumAmountByStatus(PaymentStatus.PROCESSED, start, end);

        Map<PaymentStatus, Long> countsByStatus = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus status : PaymentStatus.values()) {
            countsByStatus.put(status, 0L);
        }

        for (Object[] row : paymentRepository.countByStatus(start, end)) {
            PaymentStatus status = (PaymentStatus) row[0];
            long count = ((Number) row[1]).longValue();
            countsByStatus.put(status, count);
        }

        List<AdminPaymentStatusSummaryDTO> statusSummaries = List.of(
                new AdminPaymentStatusSummaryDTO(PaymentStatus.PENDING, countsByStatus.get(PaymentStatus.PENDING)),
                new AdminPaymentStatusSummaryDTO(PaymentStatus.PROCESSED, countsByStatus.get(PaymentStatus.PROCESSED)),
                new AdminPaymentStatusSummaryDTO(PaymentStatus.FAILED, countsByStatus.get(PaymentStatus.FAILED)),
                new AdminPaymentStatusSummaryDTO(PaymentStatus.REFUNDED, countsByStatus.get(PaymentStatus.REFUNDED)));

        long totalPayments = statusSummaries.stream().mapToLong(AdminPaymentStatusSummaryDTO::getTotal).sum();
        return new AdminFinancialOverviewDTO(
                totalIncome,
                periodIncome,
                totalPayments,
                countsByStatus.get(PaymentStatus.PENDING),
                countsByStatus.get(PaymentStatus.PROCESSED),
                countsByStatus.get(PaymentStatus.FAILED),
                countsByStatus.get(PaymentStatus.REFUNDED),
                statusSummaries);
    }

    public List<AdminPaymentByTripDTO> listPaymentsByTrip(AdminFinancialPeriod period) {
        AdminFinancialPeriod safePeriod = period == null ? AdminFinancialPeriod.ALL : period;
        LocalDateTime start = resolvePeriodStart(safePeriod);
        LocalDateTime end = resolvePeriodEnd(safePeriod);

        return paymentRepository.findAllForAdmin(start, end).stream()
                .map(AdminPaymentByTripDTO::from)
                .toList();
    }

    public List<AdminTaxRateDTO> listTaxRates() {
        return taxRateRepository.findAllOrderedForAdmin().stream()
                .map(AdminTaxRateDTO::from)
                .toList();
    }

    public AdminTaxRateDTO updateTaxRate(Integer taxRateId, AdminTaxRateCommand command) {
        if (taxRateId == null) {
            throw new IllegalArgumentException("Taxa de IVA invalida.");
        }
        if (command == null) {
            throw new IllegalArgumentException("Dados de taxa invalidos.");
        }
        if (command.rate() == null) {
            throw new IllegalArgumentException("Percentagem de IVA obrigatoria.");
        }
        if (command.rate().compareTo(BigDecimal.ZERO) < 0 || command.rate().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Taxa de IVA deve estar entre 0.0000 e 1.0000.");
        }
        if (safe(command.name()).isBlank()) {
            throw new IllegalArgumentException("Nome da taxa e obrigatorio.");
        }

        TaxRate taxRate = taxRateRepository.findById(taxRateId)
                .orElseThrow(() -> new IllegalArgumentException("Taxa de IVA nao encontrada."));

        taxRate.setName(command.name().trim());
        taxRate.setRate(command.rate());
        taxRate.setDescription(trimOrNull(command.description()));
        taxRate.setActive(command.active() == null || command.active());

        TaxRate updated = taxRateRepository.update(taxRate);
        return AdminTaxRateDTO.from(updated);
    }

    public Trip createTrip(AdminTripCommand command) {
        validateTripCommand(command);

        User client = userRepository.findById(command.clientId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado."));
        if (client.getType() != UserType.CLIENT) {
            throw new IllegalArgumentException("Utilizador selecionado para cliente e invalido.");
        }

        User driver = null;
        if (command.driverId() != null) {
            driver = userRepository.findById(command.driverId())
                    .orElseThrow(() -> new IllegalArgumentException("Motorista nao encontrado."));
            if (driver.getType() != UserType.DRIVER) {
                throw new IllegalArgumentException("Utilizador selecionado para motorista e invalido.");
            }
        }

        Route route = new Route();
        route.setOriginAddress(command.originAddress().trim());
        route.setDestinationAddress(command.destinationAddress().trim());
        route = routeRepository.save(route);

        Trip trip = new Trip();
        trip.setClient(client);
        trip.setDriver(driver);
        trip.setRoute(route);
        trip.setTripType(command.tripType() == null ? TripType.IMMEDIATE : command.tripType());
        trip.setStatus(command.status() == null ? TripStatus.PENDING : command.status());
        trip.setEstimatedPrice(command.estimatedPrice());
        trip.setFinalPrice(command.finalPrice());
        trip.setNotes(trimOrNull(command.notes()));

        if (driver != null) {
            vehicleRepository.findByDriverId(driver.getId()).stream().findFirst().ifPresent(trip::setVehicle);
        }

        return tripRepository.save(trip);
    }

    public Trip updateTrip(Integer tripId, AdminTripCommand command) {
        if (tripId == null) {
            throw new IllegalArgumentException("Viagem invalida.");
        }
        validateTripCommand(command);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem nao encontrada."));

        User client = userRepository.findById(command.clientId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado."));
        if (client.getType() != UserType.CLIENT) {
            throw new IllegalArgumentException("Utilizador selecionado para cliente e invalido.");
        }

        User driver = null;
        if (command.driverId() != null) {
            driver = userRepository.findById(command.driverId())
                    .orElseThrow(() -> new IllegalArgumentException("Motorista nao encontrado."));
            if (driver.getType() != UserType.DRIVER) {
                throw new IllegalArgumentException("Utilizador selecionado para motorista e invalido.");
            }
        }

        Route route = trip.getRoute();
        if (route == null) {
            route = new Route();
        }
        route.setOriginAddress(command.originAddress().trim());
        route.setDestinationAddress(command.destinationAddress().trim());
        if (route.getId() == null) {
            route = routeRepository.save(route);
        } else {
            route = routeRepository.update(route);
        }

        trip.setClient(client);
        trip.setDriver(driver);
        trip.setRoute(route);
        trip.setTripType(command.tripType() == null ? TripType.IMMEDIATE : command.tripType());
        trip.setStatus(command.status() == null ? TripStatus.PENDING : command.status());
        trip.setEstimatedPrice(command.estimatedPrice());
        trip.setFinalPrice(command.finalPrice());
        trip.setNotes(trimOrNull(command.notes()));

        if (driver != null) {
            Optional<Vehicle> vehicle = vehicleRepository.findByDriverId(driver.getId()).stream().findFirst();
            trip.setVehicle(vehicle.orElse(null));
        } else {
            trip.setVehicle(null);
        }

        return tripRepository.update(trip);
    }

    public void deleteTrip(Integer tripId) {
        if (tripId == null) {
            throw new IllegalArgumentException("Viagem invalida.");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem nao encontrada."));

        tripDriverRepository.findByTripId(tripId).forEach(tripDriverRepository::delete);
        tripRepository.delete(trip);
    }

    public User createUser(AdminUserCommand command) {
        validateRequiredFields(command);
        validatePassword(command.password(), true);

        String normalizedEmail = normalizeEmail(command.email());
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email ja registado.");
        }

        User user = new User();
        user.setName(command.name().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(trimOrNull(command.phone()));
        user.setStatus(command.status());
        user.setType(command.userType());
        user.setPasswordHash(passwordService.hash(command.password()));
        user.setCreatedAt(LocalDateTime.now());

        applySectionSpecificData(user, command.reference(), command.userType());
        if (command.userType() == UserType.DRIVER && user.getStatus() != AccountStatus.ACTIVE) {
            user.setAvailable(false);
        }

        return userRepository.save(user);
    }

    public User updateUser(Integer userId, AdminUserCommand command) {
        if (userId == null) {
            throw new IllegalArgumentException("Registo invalido.");
        }

        validateRequiredFields(command);
        validatePassword(command.password(), false);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Registo invalido."));

        String normalizedEmail = normalizeEmail(command.email());
        Optional<User> existing = userRepository.findByEmail(normalizedEmail);
        if (existing.isPresent() && !existing.get().getId().equals(userId)) {
            throw new IllegalArgumentException("Email ja registado.");
        }

        user.setName(command.name().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(trimOrNull(command.phone()));
        user.setStatus(command.status());
        user.setType(command.userType());
        applySectionSpecificData(user, command.reference(), command.userType());

        if (command.password() != null && !command.password().isBlank()) {
            user.setPasswordHash(passwordService.hash(command.password()));
        }

        if (command.userType() == UserType.DRIVER && user.getStatus() != AccountStatus.ACTIVE) {
            user.setAvailable(false);
        }

        return userRepository.update(user);
    }

    /**
     * Approves a pending driver account, setting status to ACTIVE.
     *
     * @param userId     id of the driver to approve
     * @param approvalNote optional internal note (may be null)
     * @throws IllegalArgumentException when user not found, not a driver, or not in PENDING status
     */
    public void approveDriver(Integer userId, String approvalNote) {
        if (userId == null) {
            throw new IllegalArgumentException("Registo invalido.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Motorista nao encontrado."));
        if (user.getType() != UserType.DRIVER) {
            throw new IllegalArgumentException("O utilizador nao e um motorista.");
        }
        if (user.getStatus() != AccountStatus.PENDING) {
            throw new IllegalArgumentException("Apenas contas pendentes podem ser aprovadas.");
        }
        user.setStatus(AccountStatus.ACTIVE);
        user.setApprovalNote(approvalNote != null ? approvalNote.trim() : null);
        userRepository.update(user);
    }

    /**
     * Rejects a pending driver account, setting status to BLOCKED with an optional note.
     *
     * @param userId     id of the driver to reject
     * @param rejectionNote optional reason shown internally (may be null)
     * @throws IllegalArgumentException when user not found, not a driver, or not in PENDING status
     */
    public void rejectDriver(Integer userId, String rejectionNote) {
        if (userId == null) {
            throw new IllegalArgumentException("Registo invalido.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Motorista nao encontrado."));
        if (user.getType() != UserType.DRIVER) {
            throw new IllegalArgumentException("O utilizador nao e um motorista.");
        }
        if (user.getStatus() != AccountStatus.PENDING) {
            throw new IllegalArgumentException("Apenas contas pendentes podem ser rejeitadas.");
        }
        user.setStatus(AccountStatus.BLOCKED);
        user.setApprovalNote(rejectionNote != null ? rejectionNote.trim() : null);
        userRepository.update(user);
    }

    /**
     * Blocks active users. If already blocked, it permanently deletes the user.
     *
     * @return true when deleted, false when blocked
     */
    public boolean blockOrDeleteUser(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Registo invalido.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Registo invalido."));

        if (user.getStatus() == AccountStatus.BLOCKED) {
            userRepository.deleteById(userId);
            return true;
        }

        user.setStatus(AccountStatus.BLOCKED);
        if (user.getType() == UserType.DRIVER) {
            user.setAvailable(false);
        }
        userRepository.update(user);
        return false;
    }

    private void validateRequiredFields(AdminUserCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Dados invalidos.");
        }

        String name = safe(command.name());
        String email = safe(command.email());
        String reference = safe(command.reference());

        if (name.isBlank()) {
            throw new IllegalArgumentException("Nome e obrigatorio.");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("Email e obrigatorio.");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Email invalido.");
        }
        if (command.status() == null) {
            throw new IllegalArgumentException("Estado e obrigatorio.");
        }
        if (command.userType() == null) {
            throw new IllegalArgumentException("Tipo de utilizador obrigatorio.");
        }
        if (command.phone() != null && command.phone().trim().length() > 40) {
            throw new IllegalArgumentException("Telefone demasiado longo.");
        }
        if (reference.isBlank()) {
            throw new IllegalArgumentException(command.userType() == UserType.DRIVER
                    ? "Numero de licenca e obrigatorio."
                    : "NIF e obrigatorio.");
        }
    }

    private void validatePassword(String password, boolean required) {
        if (!required && (password == null || password.isBlank())) {
            return;
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password e obrigatoria.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password deve ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Password deve conter pelo menos um numero.");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("Password deve conter pelo menos uma letra maiuscula.");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException("Password deve conter pelo menos uma letra minuscula.");
        }
        boolean hasSpecialCharacter = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!hasSpecialCharacter) {
            throw new IllegalArgumentException("Password deve conter pelo menos um caractere especial.");
        }
    }

    private void validateTripCommand(AdminTripCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Dados da viagem invalidos.");
        }
        if (command.clientId() == null) {
            throw new IllegalArgumentException("Cliente e obrigatorio.");
        }
        if (safe(command.originAddress()).isBlank()) {
            throw new IllegalArgumentException("Origem e obrigatoria.");
        }
        if (safe(command.destinationAddress()).isBlank()) {
            throw new IllegalArgumentException("Destino e obrigatorio.");
        }
    }

    private void applySectionSpecificData(User user, String reference, UserType userType) {
        String safeReference = safe(reference);
        if (userType == UserType.DRIVER) {
            user.setLicenseNumber(safeReference);
            user.setTaxNumber(null);
            if (user.getAvailable() == null) {
                user.setAvailable(user.getStatus() == AccountStatus.ACTIVE);
            }
            return;
        }

        user.setTaxNumber(safeReference);
        user.setLicenseNumber(null);
        user.setAvailable(false);
    }

    private String normalizeEmail(String email) {
        return safe(email).toLowerCase();
    }

    private String trimOrNull(String value) {
        String safe = safe(value);
        return safe.isBlank() ? null : safe;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private LocalDateTime resolvePeriodStart(AdminFinancialPeriod period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case DAY -> now.minusDays(1);
            case WEEK -> now.minusWeeks(1);
            case MONTH -> now.minusMonths(1);
            case YEAR -> now.minusYears(1);
            case ALL -> null;
        };
    }

    private LocalDateTime resolvePeriodEnd(AdminFinancialPeriod period) {
        if (period == AdminFinancialPeriod.ALL) {
            return null;
        }
        return LocalDateTime.now();
    }
}
