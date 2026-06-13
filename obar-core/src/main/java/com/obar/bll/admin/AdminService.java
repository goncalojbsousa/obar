package com.obar.bll.admin;

import com.obar.bll.auth.PasswordService;
import com.obar.dal.PaymentRepository;
import com.obar.dal.RouteRepository;
import com.obar.dal.TaxRateRepository;
import com.obar.dal.TripDriverRepository;
import com.obar.dal.TripRepository;
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
import java.time.Year;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Business operations used by the desktop administration screens.
 */
public class AdminService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Set<String> VEHICLE_CATEGORIES = Set.of("STANDARD", "XL", "PREMIUM");
    private static final String LETTER_PAIR = "[A-HJ-NPR-Z]{2}";
    private static final String DIGIT_PAIR = "\\d{2}";
    private static final Pattern LICENSE_PLATE_PATTERN = Pattern.compile(
            LETTER_PAIR + DIGIT_PAIR + DIGIT_PAIR
                    + "|" + DIGIT_PAIR + DIGIT_PAIR + LETTER_PAIR
                    + "|" + DIGIT_PAIR + LETTER_PAIR + DIGIT_PAIR
                    + "|" + LETTER_PAIR + DIGIT_PAIR + LETTER_PAIR);

    private final UserRepository users = new UserRepository();
    private final PasswordService passwords = new PasswordService();
    private final TripRepository trips = new TripRepository();
    private final TripDriverRepository tripDrivers = new TripDriverRepository();
    private final RouteRepository routes = new RouteRepository();
    private final VehicleRepository vehicles = new VehicleRepository();
    private final PaymentRepository payments = new PaymentRepository();
    private final TaxRateRepository taxRates = new TaxRateRepository();

    public List<AdminUserDTO> listPendingDrivers() {
        return users.findPendingDrivers().stream().map(AdminUserDTO::from).toList();
    }

    public List<AdminUserDTO> listUsersByType(UserType type) {
        if (type == null) {
            throw new IllegalArgumentException("User type is required.");
        }
        return users.findByType(type).stream().map(AdminUserDTO::from).toList();
    }

    public AdminUserDTO createUser(AdminUserCommand command) {
        validateUser(command, true);
        String email = normalizeEmail(command.email());
        if (users.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email ja registado.");
        }

        User user = new User();
        applyUserCommand(user, command, email);
        user.setPasswordHash(passwords.hash(command.password()));
        user.setCreatedAt(LocalDateTime.now());
        return AdminUserDTO.from(users.save(user));
    }

    public AdminUserDTO updateUser(Integer userId, AdminUserCommand command) {
        validateId(userId, "Registo invalido.");
        validateUser(command, false);

        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Registo invalido."));
        String email = normalizeEmail(command.email());
        users.findByEmail(email)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email ja registado.");
                });

        applyUserCommand(user, command, email);
        if (command.password() != null && !command.password().isBlank()) {
            user.setPasswordHash(passwords.hash(command.password()));
        }
        return AdminUserDTO.from(users.update(user));
    }

    public void approveDriver(Integer userId, String approvalNote) {
        decidePendingDriver(userId, AccountStatus.ACTIVE, approvalNote);
    }

    public void rejectDriver(Integer userId, String rejectionNote) {
        decidePendingDriver(userId, AccountStatus.BLOCKED, rejectionNote);
    }

    public void blockUser(Integer userId) {
        changeUserStatus(userId, AccountStatus.BLOCKED);
    }

    public void unblockUser(Integer userId) {
        changeUserStatus(userId, AccountStatus.ACTIVE);
    }

    public void deleteUser(Integer userId) {
        validateId(userId, "Registo invalido.");
        users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Registo invalido."));
        users.deleteById(userId);
    }

    public List<AdminTripDTO> listTrips() {
        return trips.findAllForAdminDashboard().stream().map(AdminTripDTO::from).toList();
    }

    public AdminTripDTO createTrip(AdminTripCommand command) {
        validateTrip(command);
        User driver = command.driverId() == null ? null : findDriver(command.driverId());

        Route route = new Route();
        route.setOriginAddress(command.originAddress().trim());
        route.setDestinationAddress(command.destinationAddress().trim());

        Trip trip = new Trip();
        trip.setClient(findClient(command.clientId()));
        trip.setDriver(driver);
        trip.setRoute(routes.save(route));
        applyTripCommand(trip, command, driver);
        return AdminTripDTO.from(trips.save(trip));
    }

    public AdminTripDTO updateTrip(Integer tripId, AdminTripCommand command) {
        validateId(tripId, "Viagem invalida.");
        validateTrip(command);

        Trip trip = trips.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem nao encontrada."));
        User driver = command.driverId() == null ? null : findDriver(command.driverId());
        Route route = trip.getRoute() == null ? new Route() : trip.getRoute();
        route.setOriginAddress(command.originAddress().trim());
        route.setDestinationAddress(command.destinationAddress().trim());

        trip.setClient(findClient(command.clientId()));
        trip.setDriver(driver);
        trip.setRoute(route.getId() == null ? routes.save(route) : routes.update(route));
        applyTripCommand(trip, command, driver);
        return AdminTripDTO.from(trips.update(trip));
    }

    public void deleteTrip(Integer tripId) {
        validateId(tripId, "Viagem invalida.");
        Trip trip = trips.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem nao encontrada."));
        tripDrivers.findByTripId(tripId).forEach(tripDrivers::delete);
        trips.delete(trip);
    }

    public List<AdminVehicleDTO> listVehicles() {
        return vehicles.findAll().stream().map(AdminVehicleDTO::from).toList();
    }

    public AdminVehicleDTO createVehicle(AdminVehicleCommand command) {
        validateVehicle(command, null);
        Vehicle vehicle = new Vehicle();
        applyVehicleCommand(vehicle, command);
        return AdminVehicleDTO.from(vehicles.save(vehicle));
    }

    public AdminVehicleDTO updateVehicle(Integer vehicleId, AdminVehicleCommand command) {
        validateId(vehicleId, "Veiculo invalido.");
        validateVehicle(command, vehicleId);
        Vehicle vehicle = findVehicle(vehicleId);
        applyVehicleCommand(vehicle, command);
        return AdminVehicleDTO.from(vehicles.update(vehicle));
    }

    public void deactivateVehicle(Integer vehicleId) {
        setVehicleActive(vehicleId, false);
    }

    public void reactivateVehicle(Integer vehicleId) {
        setVehicleActive(vehicleId, true);
    }

    public void deleteVehicle(Integer vehicleId) {
        vehicles.delete(findVehicle(vehicleId));
    }

    public AdminFinancialOverviewDTO getFinancialOverview(AdminFinancialPeriod period) {
        LocalDateTime start = periodStart(period);
        LocalDateTime end = start == null ? null : LocalDateTime.now();
        Map<PaymentStatus, Long> counts = paymentCounts(start, end);
        return new AdminFinancialOverviewDTO(
                payments.sumAmountByStatus(PaymentStatus.PROCESSED, null, null),
                payments.sumAmountByStatus(PaymentStatus.PROCESSED, start, end),
                counts.values().stream().mapToLong(Long::longValue).sum(),
                counts.get(PaymentStatus.PENDING),
                counts.get(PaymentStatus.PROCESSED),
                counts.get(PaymentStatus.FAILED),
                counts.get(PaymentStatus.REFUNDED));
    }

    public List<AdminPaymentByTripDTO> listPaymentsByTrip(AdminFinancialPeriod period) {
        LocalDateTime start = periodStart(period);
        LocalDateTime end = start == null ? null : LocalDateTime.now();
        return payments.findAllForAdmin(start, end).stream().map(AdminPaymentByTripDTO::from).toList();
    }

    public List<AdminTaxRateDTO> listTaxRates() {
        return taxRates.findAllOrderedForAdmin().stream().map(AdminTaxRateDTO::from).toList();
    }

    public AdminTaxRateDTO updateTaxRate(
            Integer taxRateId,
            String name,
            BigDecimal rate,
            String description,
            Boolean active) {
        validateId(taxRateId, "Taxa de IVA invalida.");
        if (rate == null) {
            throw new IllegalArgumentException("Percentagem de IVA obrigatoria.");
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Taxa de IVA deve estar entre 0.0000 e 1.0000.");
        }
        if (text(name).isBlank()) {
            throw new IllegalArgumentException("Nome da taxa e obrigatorio.");
        }

        TaxRate taxRate = taxRates.findById(taxRateId)
                .orElseThrow(() -> new IllegalArgumentException("Taxa de IVA nao encontrada."));
        taxRate.setName(name.trim());
        taxRate.setRate(rate);
        taxRate.setDescription(nullableText(description));
        taxRate.setActive(active == null || active);
        return AdminTaxRateDTO.from(taxRates.update(taxRate));
    }

    private void applyUserCommand(User user, AdminUserCommand command, String email) {
        user.setName(command.name().trim());
        user.setEmail(email);
        user.setPhone(nullableText(command.phone()));
        user.setStatus(command.status());
        user.setType(command.userType());

        String reference = text(command.reference());
        if (command.userType() == UserType.DRIVER) {
            user.setLicenseNumber(reference);
            user.setTaxNumber(null);
            if (user.getAvailable() == null) {
                user.setAvailable(command.status() == AccountStatus.ACTIVE);
            }
            if (command.status() != AccountStatus.ACTIVE) {
                user.setAvailable(false);
            }
        } else {
            user.setTaxNumber(reference);
            user.setLicenseNumber(null);
            user.setAvailable(false);
        }
    }

    private void validateUser(AdminUserCommand command, boolean passwordRequired) {
        if (command == null) {
            throw new IllegalArgumentException("Dados invalidos.");
        }
        if (text(command.name()).isBlank()) {
            throw new IllegalArgumentException("Nome e obrigatorio.");
        }
        if (text(command.email()).isBlank()) {
            throw new IllegalArgumentException("Email e obrigatorio.");
        }
        if (!command.email().contains("@")) {
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
        if (text(command.reference()).isBlank()) {
            throw new IllegalArgumentException(command.userType() == UserType.DRIVER
                    ? "Numero de licenca e obrigatorio."
                    : "NIF e obrigatorio.");
        }
        validatePassword(command.password(), passwordRequired);
    }

    private void validatePassword(String password, boolean required) {
        if (!required && (password == null || password.isBlank())) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password e obrigatoria.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password deve ter pelo menos 8 caracteres.");
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
        if (password.chars().noneMatch(character -> !Character.isLetterOrDigit(character))) {
            throw new IllegalArgumentException("Password deve conter pelo menos um caractere especial.");
        }
    }

    private void decidePendingDriver(Integer userId, AccountStatus status, String note) {
        User user = findUser(userId, "Motorista nao encontrado.");
        if (user.getType() != UserType.DRIVER) {
            throw new IllegalArgumentException("O utilizador nao e um motorista.");
        }
        if (user.getStatus() != AccountStatus.PENDING) {
            throw new IllegalArgumentException("Apenas contas pendentes podem ser aprovadas ou rejeitadas.");
        }
        user.setStatus(status);
        user.setApprovalNote(nullableText(note));
        users.update(user);
    }

    private void changeUserStatus(Integer userId, AccountStatus status) {
        User user = findUser(userId, "Registo invalido.");
        user.setStatus(status);
        if (user.getType() == UserType.DRIVER) {
            user.setAvailable(status == AccountStatus.ACTIVE);
        }
        users.update(user);
    }

    private void validateTrip(AdminTripCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Dados da viagem invalidos.");
        }
        if (command.clientId() == null) {
            throw new IllegalArgumentException("Cliente e obrigatorio.");
        }
        if (text(command.originAddress()).isBlank()) {
            throw new IllegalArgumentException("Origem e obrigatoria.");
        }
        if (text(command.destinationAddress()).isBlank()) {
            throw new IllegalArgumentException("Destino e obrigatorio.");
        }
    }

    private void applyTripCommand(Trip trip, AdminTripCommand command, User driver) {
        trip.setTripType(command.tripType() == null ? TripType.IMMEDIATE : command.tripType());
        trip.setStatus(command.status() == null ? TripStatus.PENDING : command.status());
        trip.setEstimatedPrice(command.estimatedPrice());
        trip.setFinalPrice(command.finalPrice());
        trip.setNotes(nullableText(command.notes()));
        trip.setVehicle(findVehicleForDriver(driver).orElse(null));
    }

    private User findClient(Integer clientId) {
        User client = findUser(clientId, "Cliente nao encontrado.");
        if (client.getType() != UserType.CLIENT) {
            throw new IllegalArgumentException("Utilizador selecionado para cliente e invalido.");
        }
        return client;
    }

    private User findDriver(Integer driverId) {
        User driver = findUser(driverId, "Motorista nao encontrado.");
        if (driver.getType() != UserType.DRIVER) {
            throw new IllegalArgumentException("Utilizador selecionado nao e motorista.");
        }
        return driver;
    }

    private Optional<Vehicle> findVehicleForDriver(User driver) {
        return driver == null ? Optional.empty() : vehicles.findByDriverId(driver.getId()).stream().findFirst();
    }

    private void validateVehicle(AdminVehicleCommand command, Integer currentVehicleId) {
        if (command == null) {
            throw new IllegalArgumentException("Dados do veiculo invalidos.");
        }
        if (text(command.brand()).isBlank()) {
            throw new IllegalArgumentException("Marca e obrigatoria.");
        }
        if (text(command.model()).isBlank()) {
            throw new IllegalArgumentException("Modelo e obrigatorio.");
        }
        if (text(command.licensePlate()).isBlank()) {
            throw new IllegalArgumentException("Matricula e obrigatoria.");
        }

        String plate = normalizeLicensePlate(command.licensePlate());
        vehicles.findByLicensePlate(plate)
                .filter(existing -> currentVehicleId == null || !currentVehicleId.equals(existing.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Matricula ja registada.");
                });
        normalizeCategory(command.category());

        if (command.year() != null
                && (command.year() < 1980 || command.year() > Year.now().getValue() + 1)) {
            throw new IllegalArgumentException("Ano do veiculo invalido.");
        }
        if (command.baseFare() != null && command.baseFare().signum() < 0) {
            throw new IllegalArgumentException("Tarifa base nao pode ser negativa.");
        }
        if (command.pricePerKm() != null && command.pricePerKm().signum() < 0) {
            throw new IllegalArgumentException("Preco por km nao pode ser negativo.");
        }
    }

    private void applyVehicleCommand(Vehicle vehicle, AdminVehicleCommand command) {
        vehicle.setDriver(findDriver(command.driverId()));
        vehicle.setBrand(text(command.brand()));
        vehicle.setModel(text(command.model()));
        vehicle.setColor(nullableText(command.color()));
        vehicle.setLicensePlate(normalizeLicensePlate(command.licensePlate()));
        vehicle.setYear(command.year());
        vehicle.setCategory(normalizeCategory(command.category()));
        vehicle.setBaseFare(command.baseFare());
        vehicle.setPricePerKm(command.pricePerKm());
        vehicle.setActive(command.active() == null || command.active());
    }

    private Vehicle findVehicle(Integer vehicleId) {
        validateId(vehicleId, "Veiculo invalido.");
        return vehicles.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Veiculo nao encontrado."));
    }

    private void setVehicleActive(Integer vehicleId, boolean active) {
        Vehicle vehicle = findVehicle(vehicleId);
        vehicle.setActive(active);
        vehicles.update(vehicle);
    }

    private Map<PaymentStatus, Long> paymentCounts(LocalDateTime start, LocalDateTime end) {
        Map<PaymentStatus, Long> counts = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus status : PaymentStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : payments.countByStatus(start, end)) {
            counts.put((PaymentStatus) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private LocalDateTime periodStart(AdminFinancialPeriod period) {
        AdminFinancialPeriod selected = period == null ? AdminFinancialPeriod.ALL : period;
        LocalDateTime now = LocalDateTime.now();
        return switch (selected) {
            case DAY -> now.minusDays(1);
            case WEEK -> now.minusWeeks(1);
            case MONTH -> now.minusMonths(1);
            case YEAR -> now.minusYears(1);
            case ALL -> null;
        };
    }

    private User findUser(Integer userId, String message) {
        validateId(userId, message);
        return users.findById(userId).orElseThrow(() -> new IllegalArgumentException(message));
    }

    private static void validateId(Integer id, String message) {
        if (id == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String normalizeCategory(String category) {
        String normalized = text(category).toUpperCase(Locale.ROOT);
        if (!VEHICLE_CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException(normalized.isBlank()
                    ? "Categoria e obrigatoria."
                    : "Categoria invalida.");
        }
        return normalized;
    }

    private static String normalizeLicensePlate(String licensePlate) {
        String compact = text(licensePlate).toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
        if (!LICENSE_PLATE_PATTERN.matcher(compact).matches()) {
            throw new IllegalArgumentException(
                    "Matricula portuguesa invalida. Use AA-00-00, 00-00-AA, 00-AA-00 ou AA-00-AA.");
        }
        return compact.substring(0, 2) + "-" + compact.substring(2, 4) + "-" + compact.substring(4, 6);
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nullableText(String value) {
        String result = text(value);
        return result.isBlank() ? null : result;
    }

    private static String normalizeEmail(String email) {
        return text(email).toLowerCase(Locale.ROOT);
    }
}
