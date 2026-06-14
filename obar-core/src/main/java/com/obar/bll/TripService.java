package com.obar.bll;

import com.obar.dal.TripRepository;
import com.obar.dal.TripDriverRepository;
import com.obar.dal.RouteRepository;
import com.obar.dal.UserRepository;
import com.obar.dal.VehicleRepository;
import com.obar.model.Route;
import com.obar.model.Trip;
import com.obar.model.TripDriver;
import com.obar.model.User;
import com.obar.model.Vehicle;
import com.obar.model.enums.TripDriverStatus;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.security.SecureRandom;

public class TripService {

    private static final Duration DRIVER_RESPONSE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration MIN_SCHEDULE_NOTICE = Duration.ofMinutes(15);
    private static final Duration SCHEDULED_TRIP_ACTIVATION_LEAD = Duration.ofMinutes(30);
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DISTANCE_SCORE_WEIGHT = 0.70;
    private static final double RATING_SCORE_WEIGHT = 0.20;
    private static final double EXPERIENCE_SCORE_WEIGHT = 0.10;
    private static final double MAX_TRIPS_FOR_EXPERIENCE_SCORE = 100.0;
    private static final SecureRandom PIN_RANDOM = new SecureRandom();

    private final TripRepository tripRepository = new TripRepository();
    private final TripDriverRepository tripDriverRepository = new TripDriverRepository();
    private final RouteRepository routeRepository = new RouteRepository();
    private final UserRepository userRepository = new UserRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();

    public Trip requestTrip(Trip trip) {
        trip.setStatus(TripStatus.PENDING);
        trip.setRequestTime(LocalDateTime.now());
        Trip savedTrip = tripRepository.save(trip);

        if (savedTrip.getTripType() == TripType.IMMEDIATE) {
            dispatchTripToNextBestAvailableDriver(savedTrip);
        }

        return savedTrip;
    }

    public Trip acceptTrip(Integer tripId, User driver) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

        if (trip.getStatus() != TripStatus.PENDING) {
            throw new IllegalStateException("Viagem não está disponível para aceitar.");
        }

        tripDriverRepository.findCurrentAssignedDriver(tripId)
                .filter(assignment -> !assignment.getDriver().getId().equals(driver.getId()))
                .ifPresent(assignment -> {
                    throw new IllegalStateException("Esta viagem está atribuída a outro motorista.");
                });

        Vehicle vehicle = vehicleRepository.findActiveByDriverIdAndCategory(driver.getId(), trip.getVehicleCategory())
                .orElseThrow(() -> new IllegalStateException("Motorista não tem veículo ativo da categoria pedida."));

        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        trip.setStatus(TripStatus.ACCEPTED);
        if (trip.getStartPin() == null || trip.getStartPin().isBlank()) {
            trip.setStartPin(generateStartPin());
        }
        Trip acceptedTrip = tripRepository.update(trip);

        markDriverAssignmentAsAccepted(tripId, driver.getId());
        if (trip.getTripType() == TripType.IMMEDIATE) {
            driver.setAvailable(false);
            userRepository.update(driver);
        }

        return acceptedTrip;
    }

    public Trip startTrip(Integer tripId, Integer driverId, String startPin) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

        if (trip.getStatus() != TripStatus.ACCEPTED) {
            throw new IllegalStateException("Viagem não está aceite.");
        }
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driverId)) {
            throw new IllegalStateException("Esta viagem pertence a outro motorista.");
        }
        if (startPin == null || !startPin.trim().equals(trip.getStartPin())) {
            throw new IllegalStateException("PIN inválido.");
        }

        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setStartTime(LocalDateTime.now());
        return tripRepository.update(trip);
    }

    public Trip completeTrip(Integer tripId, double distanceKm, int durationMin, BigDecimal finalPrice) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("Viagem não está em progresso.");
        }

        Route route = trip.getRoute();
        route.setDistanceKm((float) distanceKm);
        route.setEstimatedDurationMin(durationMin);
        trip.setRoute(routeRepository.update(route));
        trip.setFinalPrice(finalPrice);
        trip.setStatus(TripStatus.COMPLETED);
        trip.setEndTime(LocalDateTime.now());
        Trip completedTrip = tripRepository.update(trip);
        releaseDriverAfterTripEnds(completedTrip);
        return completedTrip;
    }

    public Trip cancelTrip(Integer tripId, String cancelledBy, String reason) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new IllegalStateException("Viagem já terminada.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Indica o motivo do cancelamento.");
        }

        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelledBy(cancelledBy);
        trip.setCancelReason(reason.trim());
        Trip cancelledTrip = tripRepository.update(trip);
        tripDriverRepository.updateAssignedDriversStatus(tripId, TripDriverStatus.EXPIRED);
        releaseDriverAfterTripEnds(cancelledTrip);
        return cancelledTrip;
    }

    public List<Trip> findByClient(Integer clientId) {
        return tripRepository.findByClientId(clientId);
    }

    public boolean hasActiveImmediateTrip(Integer clientId) {
        return findActiveImmediateTripByClient(clientId).isPresent();
    }

    public Optional<Trip> findActiveImmediateTripByClient(Integer clientId) {
        return findByClient(clientId).stream()
                .filter(this::isActiveImmediateTrip)
                .max(Comparator.comparing(Trip::getRequestTime));
    }

    public LocalDateTime validateScheduledTime(LocalDateTime scheduledAt) {
        if (scheduledAt == null) {
            throw new IllegalArgumentException("Escolhe a data e hora da viagem.");
        }
        if (scheduledAt.isBefore(LocalDateTime.now().plus(MIN_SCHEDULE_NOTICE))) {
            throw new IllegalArgumentException(
                    "A viagem agendada deve ser marcada com pelo menos 15 minutos de anteced\u00EAncia.");
        }
        return scheduledAt;
    }

    public List<Trip> findScheduledByClient(Integer clientId) {
        return tripRepository.findScheduledByClientId(clientId);
    }

    public List<Trip> findByDriver(Integer driverId) {
        return tripRepository.findByDriverId(driverId);
    }

    public List<Trip> findDriverHistory(Integer driverId, TripStatus status, LocalDateTime from,
            LocalDateTime to, String clientQuery, int offset, int limit) {
        return tripRepository.findDriverHistory(driverId, status, from, to, clientQuery, offset, limit);
    }

    public long countDriverHistory(Integer driverId, TripStatus status, LocalDateTime from,
            LocalDateTime to, String clientQuery) {
        return tripRepository.countDriverHistory(driverId, status, from, to, clientQuery);
    }

    public BigDecimal sumCompletedDriverHistory(Integer driverId, LocalDateTime from,
            LocalDateTime to, String clientQuery) {
        return tripRepository.sumCompletedDriverHistory(driverId, from, to, clientQuery);
    }

    public List<Trip> findPending() {
        return tripRepository.findByStatus(TripStatus.PENDING);
    }

    public List<Trip> findPendingForDriver(Integer driverId) {
        return tripRepository.findPendingByVehicleCategories(vehicleCategoriesForDriver(driverId));
    }

    public List<Trip> findPendingScheduledForDriver(Integer driverId) {
        return tripRepository.findPendingScheduledByVehicleCategories(vehicleCategoriesForDriver(driverId));
    }

    public List<Trip> findAcceptedScheduledForDriver(Integer driverId) {
        return tripRepository.findAcceptedScheduledByDriverId(driverId);
    }

    public Trip acceptScheduledTrip(Integer tripId, User driver) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem n\u00E3o encontrada."));
        if (trip.getTripType() != TripType.SCHEDULED) {
            throw new IllegalStateException("Esta viagem n\u00E3o \u00E9 programada.");
        }
        if (trip.getScheduledTime() == null || trip.getScheduledTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("A hora marcada para esta viagem j\u00E1 passou.");
        }
        return acceptTrip(tripId, driver);
    }

    public Optional<Trip> findById(Integer id) {
        return tripRepository.findById(id);
    }

    public List<Trip> findActiveImmediateTripsWithRoute() {
        return tripRepository.findActiveImmediateTripsWithRoute();
    }

    public Optional<TripDriver> findCurrentAssignmentForDriver(Integer driverId) {
        return tripDriverRepository.findCurrentAssignmentForDriver(
                driverId,
                LocalDateTime.now().plus(SCHEDULED_TRIP_ACTIVATION_LEAD));
    }

    public Optional<TripDriver> dispatchTripToNextBestAvailableDriver(Trip trip) {
        if (trip.getStatus() != TripStatus.PENDING) {
            return Optional.empty();
        }
        if (tripDriverRepository.findCurrentAssignedDriver(trip.getId()).isPresent()) {
            return Optional.empty();
        }

        return findBestAvailableDriverForTrip(trip)
                .map(driver -> assignDriver(trip, driver));
    }

    public Optional<TripDriver> rejectAssignedDriver(Integer tripId, Integer driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

        tripDriverRepository.updateAssignedDriverStatus(tripId, driverId, TripDriverStatus.REJECTED);
        return dispatchTripToNextBestAvailableDriver(trip);
    }

    public Optional<TripDriver> expireAssignedDriverAndDispatchNext(Integer tripId, Integer driverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

        tripDriverRepository.updateAssignedDriverStatus(tripId, driverId, TripDriverStatus.EXPIRED);
        return dispatchTripToNextBestAvailableDriver(trip);
    }

    public int expireTimedOutAssignmentsAndDispatchNext() {
        LocalDateTime timeoutLimit = LocalDateTime.now().minus(DRIVER_RESPONSE_TIMEOUT);
        List<TripDriver> timedOutAssignments = tripDriverRepository.findExpiredAssignedDrivers(timeoutLimit);

        for (TripDriver assignment : timedOutAssignments) {
            expireAssignedDriverAndDispatchNext(assignment.getTrip().getId(), assignment.getDriver().getId());
        }

        return timedOutAssignments.size();
    }

    public TripDriver assignDriver(Trip trip, User driver) {
        TripDriver td = new TripDriver();
        td.setTrip(trip);
        td.setDriver(driver);
        td.setStatus(TripDriverStatus.ASSIGNED);
        td.setAssignedAt(LocalDateTime.now());
        return tripDriverRepository.save(td);
    }

    private Optional<User> findBestAvailableDriverForTrip(Trip trip) {
        Route route = trip.getRoute();
        if (route == null || route.getOriginLatitude() == null || route.getOriginLongitude() == null) {
            return Optional.empty();
        }

        List<Integer> alreadyTrackedDriverIds = tripDriverRepository.findDriverIdsByTripId(trip.getId());
        List<User> compatibleDrivers = userRepository
                .findAvailableDriversByVehicleCategoryWithCurrentLocation(trip.getVehicleCategory());

        // The nearest drivers are ranked first, with rating and experience used as
        // small tie-breakers.
        return compatibleDrivers.stream()
                .filter(driver -> !alreadyTrackedDriverIds.contains(driver.getId()))
                .filter(driver -> !tripRepository.hasAcceptedScheduledTripStartingBefore(
                        driver.getId(),
                        LocalDateTime.now().plus(SCHEDULED_TRIP_ACTIVATION_LEAD)))
                .map(driver -> new DriverDispatchCandidate(
                        driver,
                        calculateDistanceKm(
                                driver.getCurrentLatitude(),
                                driver.getCurrentLongitude(),
                                route.getOriginLatitude(),
                                route.getOriginLongitude())))
                .sorted(Comparator.comparingDouble(DriverDispatchCandidate::dispatchScore))
                .map(DriverDispatchCandidate::driver)
                .findFirst();
    }

    private boolean isActiveImmediateTrip(Trip trip) {
        return trip.getTripType() == TripType.IMMEDIATE
                && (trip.getStatus() == TripStatus.PENDING
                        || trip.getStatus() == TripStatus.ACCEPTED
                        || trip.getStatus() == TripStatus.IN_PROGRESS);
    }

    private void markDriverAssignmentAsAccepted(Integer tripId, Integer driverId) {
        TripDriver assignment = tripDriverRepository.findByTripIdAndDriverId(tripId, driverId)
                .orElseGet(() -> assignDriver(
                        tripRepository.findById(tripId)
                                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada.")),
                        userRepository.findById(driverId)
                                .orElseThrow(() -> new IllegalArgumentException("Motorista não encontrado."))));

        assignment.setStatus(TripDriverStatus.ACCEPTED);
        assignment.setRespondedAt(LocalDateTime.now());
        tripDriverRepository.update(assignment);
    }

    private void releaseDriverAfterTripEnds(Trip trip) {
        User driver = trip.getDriver();
        if (driver == null) {
            return;
        }

        driver.setAvailable(true);
        if (trip.getStatus() == TripStatus.COMPLETED) {
            int completedTrips = driver.getTotalTrips() == null ? 0 : driver.getTotalTrips();
            driver.setTotalTrips(completedTrips + 1);
        }
        userRepository.update(driver);
    }

    private double calculateDistanceKm(double originLatitude, double originLongitude,
            double destinationLatitude, double destinationLongitude) {
        double originLatitudeRadians = Math.toRadians(originLatitude);
        double destinationLatitudeRadians = Math.toRadians(destinationLatitude);
        double latitudeDifference = Math.toRadians(destinationLatitude - originLatitude);
        double longitudeDifference = Math.toRadians(destinationLongitude - originLongitude);

        double haversineValue = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2)
                + Math.cos(originLatitudeRadians)
                        * Math.cos(destinationLatitudeRadians)
                        * Math.sin(longitudeDifference / 2)
                        * Math.sin(longitudeDifference / 2);

        double centralAngle = 2 * Math.atan2(Math.sqrt(haversineValue), Math.sqrt(1 - haversineValue));
        return EARTH_RADIUS_KM * centralAngle;
    }

    private String generateStartPin() {
        return String.format("%04d", PIN_RANDOM.nextInt(10_000));
    }

    private List<String> vehicleCategoriesForDriver(Integer driverId) {
        return vehicleRepository.findByDriverId(driverId).stream()
                .filter(vehicle -> Boolean.TRUE.equals(vehicle.getActive()))
                .map(Vehicle::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();
    }

    private record DriverDispatchCandidate(User driver, double pickupDistanceKm) {

        private double dispatchScore() {
            return pickupDistanceKm * DISTANCE_SCORE_WEIGHT
                    - driverRating() * RATING_SCORE_WEIGHT
                    - normalizedDriverExperience() * EXPERIENCE_SCORE_WEIGHT;
        }

        private double driverRating() {
            return driver.getAverageRating() == null ? 0.0 : driver.getAverageRating();
        }

        private double normalizedDriverExperience() {
            int totalTrips = driver.getTotalTrips() == null ? 0 : driver.getTotalTrips();
            return Math.min(totalTrips, MAX_TRIPS_FOR_EXPERIENCE_SCORE) / MAX_TRIPS_FOR_EXPERIENCE_SCORE;
        }
    }
}
