package com.obar.web.driver;

import com.obar.bll.ReviewService;
import com.obar.bll.TripService;
import com.obar.bll.UserService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.Route;
import com.obar.model.Review;
import com.obar.model.Trip;
import com.obar.model.TripDriver;
import com.obar.model.User;
import com.obar.model.enums.UserType;
import com.obar.web.maps.client.MapsServiceClient;
import com.obar.web.maps.dto.request.RouteEstimateRequest;
import com.obar.web.maps.dto.response.RouteEstimateResponse;
import com.obar.web.session.WebSessionHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class DriverApiController {

        private static final Duration DRIVER_RESPONSE_TIMEOUT = Duration.ofSeconds(30);

        private final TripService tripService;
        private final ReviewService reviewService;
        private final UserService userService;
        private final MapsServiceClient mapsServiceClient;

        public DriverApiController(TripService tripService, ReviewService reviewService, UserService userService,
                        MapsServiceClient mapsServiceClient) {
                this.tripService = tripService;
                this.reviewService = reviewService;
                this.userService = userService;
                this.mapsServiceClient = mapsServiceClient;
        }

        @GetMapping("/api/driver/assignment")
        public DriverAssignmentResponse currentAssignment(HttpSession session) {
                AuthenticatedUserDto currentUser = requireDriver(session);
                return tripService.findCurrentAssignmentForDriver(currentUser.id())
                                .map(this::toAssignmentResponse)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Sem viagem atribuída."));
        }

        @PostMapping("/api/driver/assignment/accept")
        public DriverAssignmentResponse acceptAssignment(@RequestBody(required = false) DriverLocationUpdateRequest location,
                        HttpSession session) {
                AuthenticatedUserDto currentUser = requireDriver(session);
                TripDriver assignment = currentAssignmentEntity(currentUser.id());
                Trip assignedTrip = assignment.getTrip();
                User driver = userService.findById(currentUser.id())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                if (location != null) {
                        userService.updateDriverCurrentLocation(currentUser.id(), (float) location.lat(),
                                        (float) location.lng());
                        driver.setCurrentLatitude((float) location.lat());
                        driver.setCurrentLongitude((float) location.lng());
                }
                Trip acceptedTrip = tripService.acceptTrip(assignedTrip.getId(), driver);
                assignment.setTrip(acceptedTrip);
                assignment.setDriver(driver);
                return toAssignmentResponse(acceptedTrip, assignment.getAssignedAt(), "ACCEPTED", driver);
        }

        @PostMapping("/api/driver/assignment/start")
        public DriverAssignmentResponse startTrip(@RequestBody DriverStartTripRequest request, HttpSession session) {
                AuthenticatedUserDto currentUser = requireDriver(session);
                TripDriver assignment = currentAssignmentEntity(currentUser.id());
                Trip startedTrip;
                try {
                        startedTrip = tripService.startTrip(
                                        assignment.getTrip().getId(),
                                        currentUser.id(),
                                        request.pin());
                } catch (IllegalArgumentException | IllegalStateException exception) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
                }
                assignment.setTrip(startedTrip);
                return toAssignmentResponse(startedTrip, assignment.getAssignedAt(), "IN_PROGRESS",
                                assignment.getDriver());
        }

        @PostMapping("/api/driver/assignment/complete")
        public DriverActionResponse completeTrip(@RequestBody DriverCompleteTripRequest request, HttpSession session) {
                AuthenticatedUserDto currentUser = requireDriver(session);
                TripDriver assignment = currentAssignmentEntity(currentUser.id());
                Trip trip = assignment.getTrip();

                if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolhe uma nota entre 1 e 5.");
                }
                if (trip.getDriver() == null || !trip.getDriver().getId().equals(currentUser.id())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta viagem pertence a outro motorista.");
                }

                try {
                        Trip completedTrip = tripService.completeTrip(trip.getId());
                        Review review = new Review();
                        review.setTrip(completedTrip);
                        review.setReviewer(assignment.getDriver());
                        review.setReviewed(completedTrip.getClient());
                        review.setRating(request.rating());
                        review.setReviewerType("DRIVER");
                        reviewService.addReview(review);
                } catch (IllegalArgumentException | IllegalStateException exception) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
                }

                return new DriverActionResponse("Viagem concluída e cliente avaliado.");
        }

        @PostMapping("/api/driver/assignment/reject")
        public DriverActionResponse rejectAssignment(HttpSession session) {
                AuthenticatedUserDto currentUser = requireDriver(session);
                TripDriver assignment = currentAssignmentEntity(currentUser.id());
                tripService.rejectAssignedDriver(assignment.getTrip().getId(), currentUser.id());
                return new DriverActionResponse("Viagem rejeitada.");
        }

        @PostMapping("/api/driver/assignment/cancel")
        public DriverActionResponse cancelTrip(@RequestBody DriverCancelTripRequest request, HttpSession session) {
                AuthenticatedUserDto currentUser = requireDriver(session);
                TripDriver assignment = currentAssignmentEntity(currentUser.id());
                Trip trip = assignment.getTrip();
                if (trip.getDriver() == null || !trip.getDriver().getId().equals(currentUser.id())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não podes cancelar esta viagem.");
                }
                tripService.cancelTrip(trip.getId(), "DRIVER", requireReason(request == null ? null : request.reason()));
                return new DriverActionResponse("Viagem cancelada.");
        }

        @PostMapping("/api/driver/location")
        public DriverActionResponse updateLocation(@RequestBody DriverLocationUpdateRequest request,
                        HttpSession session) {
                AuthenticatedUserDto currentUser = requireDriver(session);
                userService.updateDriverCurrentLocation(currentUser.id(), (float) request.lat(), (float) request.lng());
                return new DriverActionResponse("Localização atualizada.");
        }

        @GetMapping("/api/driver/online")
        public DriverOnlineResponse onlineStatus(HttpSession session) {
                AuthenticatedUserDto currentUser = requireDriver(session);
                User driver = userService.findById(currentUser.id())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                return new DriverOnlineResponse(Boolean.TRUE.equals(driver.getOnline()));
        }

        @PostMapping("/api/driver/online")
        public DriverOnlineResponse updateOnlineStatus(@RequestBody DriverOnlineRequest request,
                        HttpSession session) {
                AuthenticatedUserDto currentUser = requireDriver(session);
                if (tripService.findCurrentAssignmentForDriver(currentUser.id()).isPresent()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "Termina ou cancela a viagem antes de alterares o estado online.");
                }
                try {
                        if (request.online() && request.lat() != null && request.lng() != null) {
                                userService.updateDriverCurrentLocation(
                                                currentUser.id(),
                                                request.lat().floatValue(),
                                                request.lng().floatValue());
                        }
                        userService.setDriverOnline(currentUser.id(), request.online());
                } catch (IllegalArgumentException | IllegalStateException exception) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
                }
                return new DriverOnlineResponse(request.online());
        }

        @GetMapping("/api/driver/map-snapshot")
        public DriverMapSnapshotResponse mapSnapshot(HttpSession session) {
                requireDriver(session);
                List<DriverMapPointResponse> drivers = userService.findOnlineDriversWithCurrentLocation().stream()
                                .map(driver -> new DriverMapPointResponse(
                                                driver.getId(),
                                                driver.getName(),
                                                "DRIVER",
                                                driver.getCurrentLatitude(),
                                                driver.getCurrentLongitude()))
                                .toList();

                List<DriverMapPointResponse> clients = tripService.findActiveImmediateTripsWithRoute().stream()
                                .filter(trip -> trip.getRoute().getOriginLatitude() != null
                                                && trip.getRoute().getOriginLongitude() != null)
                                .map(trip -> new DriverMapPointResponse(
                                                trip.getClient().getId(),
                                                trip.getClient().getName(),
                                                "CLIENT",
                                                trip.getRoute().getOriginLatitude(),
                                                trip.getRoute().getOriginLongitude()))
                                .toList();

                return new DriverMapSnapshotResponse(drivers, clients);
        }

        private TripDriver currentAssignmentEntity(Integer driverId) {
                return tripService.findCurrentAssignmentForDriver(driverId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Sem viagem atribuída."));
        }

        private DriverAssignmentResponse toAssignmentResponse(TripDriver assignment) {
                return toAssignmentResponse(assignment.getTrip(), assignment.getAssignedAt(),
                                assignment.getTrip().getStatus().name(), assignment.getDriver());
        }

        private DriverAssignmentResponse toAssignmentResponse(Trip trip, LocalDateTime assignedAt, String status,
                        User driver) {
                Route route = trip.getRoute();
                boolean pickupRoute = "ACCEPTED".equals(status)
                                && driver != null
                                && driver.getCurrentLatitude() != null
                                && driver.getCurrentLongitude() != null;

                double originLat = pickupRoute ? driver.getCurrentLatitude() : route.getOriginLatitude();
                double originLng = pickupRoute ? driver.getCurrentLongitude() : route.getOriginLongitude();
                double destinationLat = pickupRoute ? route.getOriginLatitude() : route.getDestinationLatitude();
                double destinationLng = pickupRoute ? route.getOriginLongitude() : route.getDestinationLongitude();
                String originAddress = pickupRoute ? "A tua localização" : route.getOriginAddress();
                String destinationAddress = pickupRoute ? route.getOriginAddress() : route.getDestinationAddress();

                RouteEstimateResponse estimate = mapsServiceClient.estimate(new RouteEstimateRequest(
                                originLat,
                                originLng,
                                destinationLat,
                                destinationLng,
                                originAddress,
                                destinationAddress,
                                trip.getVehicleCategory(),
                                null));

                long secondsLeft = Math.max(0,
                                DRIVER_RESPONSE_TIMEOUT.minus(Duration.between(assignedAt, LocalDateTime.now()))
                                                .toSeconds());

                return new DriverAssignmentResponse(
                                trip.getId(),
                                trip.getClient().getName(),
                                trip.getClient().getAverageRating(),
                                originAddress,
                                destinationAddress,
                                originLat,
                                originLng,
                                destinationLat,
                                destinationLng,
                                estimate.distanceKm(),
                                estimate.durationMin(),
                                estimate.estimatedPrice(),
                                trip.getVehicleCategory(),
                                status,
                                assignedAt,
                                secondsLeft,
                                pickupRoute ? "PICKUP" : "TRIP",
                                estimate.geometry());
        }

        private AuthenticatedUserDto requireDriver(HttpSession session) {
                AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                if (currentUser.type() != UserType.DRIVER) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Área reservada a motoristas.");
                }
                return currentUser;
        }

        private String requireReason(String reason) {
                if (reason == null || reason.isBlank()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica o motivo do cancelamento.");
                }
                return reason.trim();
        }
}
