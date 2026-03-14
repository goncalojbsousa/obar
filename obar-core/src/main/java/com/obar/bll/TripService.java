package com.obar.bll;

import com.obar.dal.TripRepository;
import com.obar.dal.TripDriverRepository;
import com.obar.model.Trip;
import com.obar.model.TripDriver;
import com.obar.model.User;
import com.obar.model.enums.TripDriverStatus;
import com.obar.model.enums.TripStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class TripService {

    private final TripRepository tripRepository = new TripRepository();
    private final TripDriverRepository tripDriverRepository = new TripDriverRepository();

    public Trip requestTrip(Trip trip) {
        trip.setStatus(TripStatus.PENDING);
        trip.setRequestTime(LocalDateTime.now());
        return tripRepository.save(trip);
    }

    public Trip acceptTrip(Integer tripId, User driver) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

        if (trip.getStatus() != TripStatus.PENDING) {
            throw new IllegalStateException("Viagem não está disponível para aceitar.");
        }

        trip.setDriver(driver);
        trip.setStatus(TripStatus.ACCEPTED);
        return tripRepository.update(trip);
    }

    public Trip startTrip(Integer tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

        if (trip.getStatus() != TripStatus.ACCEPTED) {
            throw new IllegalStateException("Viagem não está aceite.");
        }

        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setStartTime(LocalDateTime.now());
        return tripRepository.update(trip);
    }

    public Trip completeTrip(Integer tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("Viagem não está em progresso.");
        }

        trip.setStatus(TripStatus.COMPLETED);
        trip.setEndTime(LocalDateTime.now());
        return tripRepository.update(trip);
    }

    public Trip cancelTrip(Integer tripId, String cancelledBy, String reason) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new IllegalStateException("Viagem já terminada.");
        }

        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelledBy(cancelledBy);
        trip.setCancelReason(reason);
        return tripRepository.update(trip);
    }

    public List<Trip> findByClient(Integer clientId) {
        return tripRepository.findByClientId(clientId);
    }

    public List<Trip> findByDriver(Integer driverId) {
        return tripRepository.findByDriverId(driverId);
    }

    public List<Trip> findPending() {
        return tripRepository.findByStatus(TripStatus.PENDING);
    }

    public Optional<Trip> findById(Integer id) {
        return tripRepository.findById(id);
    }

    public TripDriver assignDriver(Trip trip, User driver) {
        TripDriver td = new TripDriver();
        td.setTrip(trip);
        td.setDriver(driver);
        td.setStatus(TripDriverStatus.ASSIGNED);
        td.setAssignedAt(LocalDateTime.now());
        return tripDriverRepository.save(td);
    }
}