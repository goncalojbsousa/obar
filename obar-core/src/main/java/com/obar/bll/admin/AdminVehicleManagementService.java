package com.obar.bll.admin;

import com.obar.dal.UserRepository;
import com.obar.dal.VehicleRepository;
import com.obar.model.User;
import com.obar.model.Vehicle;
import com.obar.model.enums.UserType;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Admin BLL use cases for vehicles.
 */
final class AdminVehicleManagementService {

    private static final Set<String> SUPPORTED_CATEGORIES = Set.of("STANDARD", "XL", "PREMIUM");
    private static final String LETTER_PAIR = "[A-HJ-NPR-Z]{2}";
    private static final String DIGIT_PAIR = "\\d{2}";
    private static final Pattern PORTUGUESE_LICENSE_PLATE_PATTERN = Pattern.compile(
            LETTER_PAIR + DIGIT_PAIR + DIGIT_PAIR
                    + "|" + DIGIT_PAIR + DIGIT_PAIR + LETTER_PAIR
                    + "|" + DIGIT_PAIR + LETTER_PAIR + DIGIT_PAIR
                    + "|" + LETTER_PAIR + DIGIT_PAIR + LETTER_PAIR);

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    AdminVehicleManagementService(UserRepository userRepository, VehicleRepository vehicleRepository) {
        if (userRepository == null) {
            throw new IllegalArgumentException("UserRepository must not be null.");
        }
        if (vehicleRepository == null) {
            throw new IllegalArgumentException("VehicleRepository must not be null.");
        }
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    List<AdminVehicleDTO> listVehicles() {
        return vehicleRepository.findAll().stream()
                .map(AdminVehicleDTO::from)
                .toList();
    }

    AdminVehicleDTO createVehicle(AdminVehicleCommand command) {
        validateVehicleCommand(command, null);

        Vehicle vehicle = new Vehicle();
        applyCommand(vehicle, command);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return AdminVehicleDTO.from(savedVehicle);
    }

    AdminVehicleDTO updateVehicle(Integer vehicleId, AdminVehicleCommand command) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Veiculo invalido.");
        }
        validateVehicleCommand(command, vehicleId);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Veiculo nao encontrado."));
        applyCommand(vehicle, command);

        Vehicle updatedVehicle = vehicleRepository.update(vehicle);
        return AdminVehicleDTO.from(updatedVehicle);
    }

    void deactivateVehicle(Integer vehicleId) {
        Vehicle vehicle = findVehicle(vehicleId);
        vehicle.setActive(false);
        vehicleRepository.update(vehicle);
    }

    void reactivateVehicle(Integer vehicleId) {
        Vehicle vehicle = findVehicle(vehicleId);
        vehicle.setActive(true);
        vehicleRepository.update(vehicle);
    }

    void deleteVehicle(Integer vehicleId) {
        Vehicle vehicle = findVehicle(vehicleId);
        vehicleRepository.delete(vehicle);
    }

    private void applyCommand(Vehicle vehicle, AdminVehicleCommand command) {
        vehicle.setDriver(findDriver(command.driverId()));
        vehicle.setBrand(AdminTextSanitizer.safe(command.brand()));
        vehicle.setModel(AdminTextSanitizer.safe(command.model()));
        vehicle.setColor(AdminTextSanitizer.trimOrNull(command.color()));
        vehicle.setLicensePlate(normalizePortugueseLicensePlate(command.licensePlate()));
        vehicle.setYear(command.year());
        vehicle.setCategory(normalizeCategory(command.category()));
        vehicle.setBaseFare(command.baseFare());
        vehicle.setPricePerKm(command.pricePerKm());
        vehicle.setActive(command.active() == null || command.active());
    }

    private User findDriver(Integer driverId) {
        if (driverId == null) {
            throw new IllegalArgumentException("Motorista e obrigatorio.");
        }
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Motorista nao encontrado."));
        if (driver.getType() != UserType.DRIVER) {
            throw new IllegalArgumentException("Utilizador selecionado nao e motorista.");
        }
        return driver;
    }

    private Vehicle findVehicle(Integer vehicleId) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Veiculo invalido.");
        }
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Veiculo nao encontrado."));
    }

    private void validateVehicleCommand(AdminVehicleCommand command, Integer currentVehicleId) {
        if (command == null) {
            throw new IllegalArgumentException("Dados do veiculo invalidos.");
        }
        if (AdminTextSanitizer.safe(command.brand()).isBlank()) {
            throw new IllegalArgumentException("Marca e obrigatoria.");
        }
        if (AdminTextSanitizer.safe(command.model()).isBlank()) {
            throw new IllegalArgumentException("Modelo e obrigatorio.");
        }
        if (AdminTextSanitizer.safe(command.licensePlate()).isBlank()) {
            throw new IllegalArgumentException("Matricula e obrigatoria.");
        }
        String normalizedLicensePlate = normalizePortugueseLicensePlate(command.licensePlate());
        validateUniqueLicensePlate(currentVehicleId, normalizedLicensePlate);
        normalizeCategory(command.category());
        if (command.year() != null) {
            int nextYear = Year.now().getValue() + 1;
            if (command.year() < 1980 || command.year() > nextYear) {
                throw new IllegalArgumentException("Ano do veiculo invalido.");
            }
        }
        if (command.baseFare() != null && command.baseFare().signum() < 0) {
            throw new IllegalArgumentException("Tarifa base nao pode ser negativa.");
        }
        if (command.pricePerKm() != null && command.pricePerKm().signum() < 0) {
            throw new IllegalArgumentException("Preco por km nao pode ser negativo.");
        }
    }

    private void validateUniqueLicensePlate(Integer currentVehicleId, String normalizedLicensePlate) {
        Optional<Vehicle> existingVehicle = vehicleRepository.findByLicensePlate(normalizedLicensePlate);
        if (existingVehicle.isEmpty()) {
            return;
        }

        boolean sameVehicle = currentVehicleId != null && currentVehicleId.equals(existingVehicle.get().getId());
        if (!sameVehicle) {
            throw new IllegalArgumentException("Matricula ja registada.");
        }
    }

    private static String normalizeCategory(String category) {
        String normalizedCategory = AdminTextSanitizer.safe(category).toUpperCase();
        if (normalizedCategory.isBlank()) {
            throw new IllegalArgumentException("Categoria e obrigatoria.");
        }
        if (!SUPPORTED_CATEGORIES.contains(normalizedCategory)) {
            throw new IllegalArgumentException("Categoria invalida.");
        }
        return normalizedCategory;
    }

    private static String normalizePortugueseLicensePlate(String licensePlate) {
        String compactLicensePlate = AdminTextSanitizer.safe(licensePlate)
                .toUpperCase()
                .replace("-", "")
                .replace(" ", "");
        if (!PORTUGUESE_LICENSE_PLATE_PATTERN.matcher(compactLicensePlate).matches()) {
            throw new IllegalArgumentException(
                    "Matricula portuguesa invalida. Use AA-00-00, 00-00-AA, 00-AA-00 ou AA-00-AA.");
        }
        return compactLicensePlate.substring(0, 2) + "-"
                + compactLicensePlate.substring(2, 4) + "-"
                + compactLicensePlate.substring(4, 6);
    }
}
