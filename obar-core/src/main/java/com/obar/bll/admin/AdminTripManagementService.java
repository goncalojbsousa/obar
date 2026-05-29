package com.obar.bll.admin;

import com.obar.dal.RouteRepository;
import com.obar.dal.TripDriverRepository;
import com.obar.dal.TripRepository;
import com.obar.dal.UserRepository;
import com.obar.dal.VehicleRepository;
import com.obar.model.Route;
import com.obar.model.Trip;
import com.obar.model.User;
import com.obar.model.Vehicle;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import com.obar.model.enums.UserType;

import java.util.List;
import java.util.Optional;

/**
 * Admin BLL use cases for trip management.
 */
final class AdminTripManagementService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripDriverRepository tripDriverRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;

    AdminTripManagementService(
            UserRepository userRepository,
            TripRepository tripRepository,
            TripDriverRepository tripDriverRepository,
            RouteRepository routeRepository,
            VehicleRepository vehicleRepository) {
        if (userRepository == null) {
            throw new IllegalArgumentException("UserRepository must not be null.");
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
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.tripDriverRepository = tripDriverRepository;
        this.routeRepository = routeRepository;
        this.vehicleRepository = vehicleRepository;
    }

    List<AdminTripDTO> listTrips() {
        return tripRepository.findAllForAdminDashboard().stream()
                .map(AdminTripDTO::from)
                .toList();
    }

    AdminTripDTO createTrip(AdminTripCommand command) {
        validateTripCommand(command);

        User client = findClient(command.clientId());
        User driver = command.driverId() == null ? null : findDriver(command.driverId());

        Route route = new Route();
        route.setOriginAddress(command.originAddress().trim());
        route.setDestinationAddress(command.destinationAddress().trim());
        Route savedRoute = routeRepository.save(route);

        Trip trip = new Trip();
        trip.setClient(client);
        trip.setDriver(driver);
        trip.setRoute(savedRoute);
        trip.setTripType(command.tripType() == null ? TripType.IMMEDIATE : command.tripType());
        trip.setStatus(command.status() == null ? TripStatus.PENDING : command.status());
        trip.setEstimatedPrice(command.estimatedPrice());
        trip.setFinalPrice(command.finalPrice());
        trip.setNotes(AdminTextSanitizer.trimOrNull(command.notes()));

        if (driver != null) {
            vehicleRepository.findByDriverId(driver.getId()).stream().findFirst().ifPresent(trip::setVehicle);
        }

        Trip savedTrip = tripRepository.save(trip);
        return AdminTripDTO.from(savedTrip);
    }

    AdminTripDTO updateTrip(Integer tripId, AdminTripCommand command) {
        if (tripId == null) {
            throw new IllegalArgumentException("Viagem invalida.");
        }
        validateTripCommand(command);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem nao encontrada."));

        User client = findClient(command.clientId());
        User driver = command.driverId() == null ? null : findDriver(command.driverId());

        Route route = trip.getRoute();
        if (route == null) {
            route = new Route();
        }
        route.setOriginAddress(command.originAddress().trim());
        route.setDestinationAddress(command.destinationAddress().trim());

        Route savedRoute = route.getId() == null
                ? routeRepository.save(route)
                : routeRepository.update(route);

        trip.setClient(client);
        trip.setDriver(driver);
        trip.setRoute(savedRoute);
        trip.setTripType(command.tripType() == null ? TripType.IMMEDIATE : command.tripType());
        trip.setStatus(command.status() == null ? TripStatus.PENDING : command.status());
        trip.setEstimatedPrice(command.estimatedPrice());
        trip.setFinalPrice(command.finalPrice());
        trip.setNotes(AdminTextSanitizer.trimOrNull(command.notes()));
        trip.setVehicle(findVehicleForDriver(driver).orElse(null));

        Trip updatedTrip = tripRepository.update(trip);
        return AdminTripDTO.from(updatedTrip);
    }

    void deleteTrip(Integer tripId) {
        if (tripId == null) {
            throw new IllegalArgumentException("Viagem invalida.");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem nao encontrada."));

        tripDriverRepository.findByTripId(tripId).forEach(tripDriverRepository::delete);
        tripRepository.delete(trip);
    }

    private User findClient(Integer clientId) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado."));
        if (client.getType() != UserType.CLIENT) {
            throw new IllegalArgumentException("Utilizador selecionado para cliente e invalido.");
        }
        return client;
    }

    private User findDriver(Integer driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Motorista nao encontrado."));
        if (driver.getType() != UserType.DRIVER) {
            throw new IllegalArgumentException("Utilizador selecionado para motorista e invalido.");
        }
        return driver;
    }

    private Optional<Vehicle> findVehicleForDriver(User driver) {
        if (driver == null) {
            return Optional.empty();
        }
        return vehicleRepository.findByDriverId(driver.getId()).stream().findFirst();
    }

    private void validateTripCommand(AdminTripCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Dados da viagem invalidos.");
        }
        if (command.clientId() == null) {
            throw new IllegalArgumentException("Cliente e obrigatorio.");
        }
        if (AdminTextSanitizer.safe(command.originAddress()).isBlank()) {
            throw new IllegalArgumentException("Origem e obrigatoria.");
        }
        if (AdminTextSanitizer.safe(command.destinationAddress()).isBlank()) {
            throw new IllegalArgumentException("Destino e obrigatorio.");
        }
    }
}
