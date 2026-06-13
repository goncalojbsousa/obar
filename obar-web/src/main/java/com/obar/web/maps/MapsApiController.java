package com.obar.web.maps;

import com.obar.bll.RouteService;
import com.obar.bll.ReviewService;
import com.obar.bll.TaxRateService;
import com.obar.bll.TaxRateService.FareQuote;
import com.obar.bll.TripService;
import com.obar.bll.UserService;
import com.obar.bll.VehicleService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.Route;
import com.obar.model.Trip;
import com.obar.model.User;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import com.obar.web.maps.client.MapsServiceClient;
import com.obar.web.maps.client.MapsServiceClient.LocationSuggestionResponse;
import com.obar.web.maps.client.MapsServiceClient.RouteEstimateRequest;
import com.obar.web.maps.client.MapsServiceClient.RouteEstimateResponse;
import com.obar.web.session.WebSessionHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class MapsApiController {

    private final MapsServiceClient mapsServiceClient;
    private final RouteService routeService;
    private final TripService tripService;
    private final UserService userService;
    private final TaxRateService taxRateService;
    private final ReviewService reviewService;

    public MapsApiController(
            MapsServiceClient mapsServiceClient,
            RouteService routeService,
            TripService tripService,
            UserService userService,
            TaxRateService taxRateService,
            ReviewService reviewService) {
        this.mapsServiceClient = mapsServiceClient;
        this.routeService = routeService;
        this.tripService = tripService;
        this.userService = userService;
        this.taxRateService = taxRateService;
        this.reviewService = reviewService;
    }

    @GetMapping("/api/locations/search")
    public List<LocationSuggestionResponse> searchLocations(@RequestParam("text") String text) {
        return mapsServiceClient.autocomplete(text);
    }

    @GetMapping("/api/locations/reverse")
    public LocationSuggestionResponse reverseLocation(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng) {
        return mapsServiceClient.reverseGeocode(lat, lng);
    }

    @PostMapping("/api/routes/estimate")
    public FareEstimateResponse estimateRoute(@RequestBody RouteEstimateRequest request, HttpSession session) {
        User client = currentClient(session);
        RouteEstimateResponse estimate = mapsServiceClient.estimate(request);
        return toFareEstimate(estimate, taxRateService.quote(estimate.estimatedPrice(), client.getTaxNumber()));
    }

    @PostMapping("/api/trips/request")
    public TripRequestResponse requestTrip(@RequestBody RouteEstimateRequest request, HttpSession session) {
        return createTrip(request, session, TripType.IMMEDIATE);
    }

    @PostMapping("/api/trips/schedule")
    public TripRequestResponse scheduleTrip(@RequestBody RouteEstimateRequest request, HttpSession session) {
        return createTrip(request, session, TripType.SCHEDULED);
    }

    private TripRequestResponse createTrip(RouteEstimateRequest request, HttpSession session, TripType tripType) {
        User client = currentClient(session);

        if (Boolean.TRUE.equals(client.getOnline())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Fica offline antes de pedires uma viagem.");
        }
        if (tripType == TripType.IMMEDIATE && tripService.hasActiveImmediateTrip(client.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já tens uma viagem ativa ou à espera de motorista.");
        }
        LocalDateTime scheduledAt = tripType == TripType.SCHEDULED
                ? tripService.validateScheduledTime(request.scheduledAt())
                : null;

        String vehicleCategory = VehicleService.normalizeCategory(request.vehicleCategory());
        if (tripType == TripType.IMMEDIATE && !userService.hasOnlineAvailableDriverForCategory(vehicleCategory)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não existem motoristas online e disponíveis para esta categoria. Tenta novamente mais tarde.");
        }
        RouteEstimateResponse estimate = mapsServiceClient.estimate(request);
        FareQuote fare = taxRateService.quote(estimate.estimatedPrice(), client.getTaxNumber());

        Route route = new Route();
        route.setOriginAddress(blankToDefault(request.originAddress(), "Localização atual"));
        route.setDestinationAddress(blankToDefault(request.destinationAddress(), "Destino"));
        route.setOriginLatitude((float) request.originLat());
        route.setOriginLongitude((float) request.originLng());
        route.setDestinationLatitude((float) request.destinationLat());
        route.setDestinationLongitude((float) request.destinationLng());
        route.setDistanceKm((float) estimate.distanceKm());
        route.setEstimatedDurationMin(estimate.durationMin());
        Route savedRoute = routeService.save(route);

        Trip trip = new Trip();
        trip.setClient(client);
        trip.setRoute(savedRoute);
        trip.setTripType(tripType);
        trip.setScheduledTime(scheduledAt);
        trip.setVehicleCategory(vehicleCategory);
        trip.setEstimatedPrice(fare.totalAmount());
        trip.setTaxRateApplied(fare.taxRate());
        Trip savedTrip = tripService.requestTrip(trip);

        return new TripRequestResponse(
                savedTrip.getId(),
                savedRoute.getId(),
                estimate.distanceKm(),
                estimate.durationMin(),
                fare.totalAmount(),
                fare.taxRate(),
                savedTrip.getVehicleCategory(),
                savedTrip.getTripType().name(),
                savedTrip.getScheduledTime(),
                savedTrip.getStatus().name());
    }

    @GetMapping("/api/trips/active")
    public ActiveTripResponse activeTrip(HttpSession session) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return tripService.findActiveImmediateTripByClient(currentUser.id())
                .map(this::toActiveTripResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não existe viagem ativa."));
    }

    @GetMapping("/api/trips/review-pending")
    public PendingReviewResponse pendingReview(HttpSession session) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Trip trip = reviewService.findPendingClientReview(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sem avaliacao pendente."));
        return new PendingReviewResponse(
                trip.getId(),
                trip.getDriver().getName(),
                trip.getDriver().getPhotoUrl(),
                trip.getRoute().getDestinationAddress(),
                trip.getFinalPrice() == null ? trip.getEstimatedPrice() : trip.getFinalPrice());
    }

    @PostMapping("/api/trips/{tripId}/review")
    public ReviewResponse reviewDriver(@PathVariable Integer tripId,
            @RequestBody TripReviewRequest request,
            HttpSession session) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        try {
            reviewService.addClientReview(
                    tripId,
                    currentUser.id(),
                    request == null ? null : request.rating(),
                    request == null ? null : request.comment());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
        return new ReviewResponse("Obrigado pela tua avaliacao.");
    }

    @PostMapping("/api/trips/{tripId}/cancel")
    public TripCancellationResponse cancelTrip(@PathVariable Integer tripId,
            @RequestBody TripCancellationRequest request,
            HttpSession session) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Trip trip = tripService.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viagem não encontrada."));

        if (!trip.getClient().getId().equals(currentUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não podes cancelar esta viagem.");
        }

        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta viagem já não pode ser cancelada.");
        }

        Trip cancelledTrip = tripService.cancelTrip(tripId, "CLIENT",
                requireReason(request == null ? null : request.reason()));
        return new TripCancellationResponse(
                cancelledTrip.getId(),
                cancelledTrip.getStatus().name(),
                "Viagem cancelada.");
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica o motivo do cancelamento.");
        }
        return reason.trim();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private ActiveTripResponse toActiveTripResponse(Trip trip) {
        Route route = trip.getRoute();
        return new ActiveTripResponse(
                trip.getId(),
                route.getId(),
                trip.getStatus().name(),
                route.getOriginAddress(),
                route.getDestinationAddress(),
                route.getOriginLatitude(),
                route.getOriginLongitude(),
                route.getDestinationLatitude(),
                route.getDestinationLongitude(),
                route.getDistanceKm(),
                route.getEstimatedDurationMin(),
                trip.getEstimatedPrice(),
                trip.getTaxRateApplied(),
                trip.getVehicleCategory(),
                trip.getStatus() == TripStatus.ACCEPTED ? trip.getStartPin() : null,
                driverArrivalMin(trip));
    }

    private Integer driverArrivalMin(Trip trip) {
        User driver = trip.getDriver();
        Route route = trip.getRoute();
        if (trip.getStatus() != TripStatus.ACCEPTED
                || driver == null
                || driver.getCurrentLatitude() == null
                || driver.getCurrentLongitude() == null) {
            return null;
        }

        try {
            return mapsServiceClient.estimate(new RouteEstimateRequest(
                    driver.getCurrentLatitude(),
                    driver.getCurrentLongitude(),
                    route.getOriginLatitude(),
                    route.getOriginLongitude(),
                    "Localização do motorista",
                    route.getOriginAddress(),
                    trip.getVehicleCategory(),
                    null)).durationMin();
        } catch (ResponseStatusException exception) {
            return null;
        }
    }

    private User currentClient(HttpSession session) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return userService.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private FareEstimateResponse toFareEstimate(RouteEstimateResponse estimate, FareQuote fare) {
        return new FareEstimateResponse(
                estimate.distanceKm(),
                estimate.durationMin(),
                fare.netAmount(),
                fare.taxRate(),
                fare.taxAmount(),
                fare.totalAmount(),
                estimate.geometry());
    }

    public record ActiveTripResponse(
            Integer tripId,
            Integer routeId,
            String status,
            String originAddress,
            String destinationAddress,
            double originLat,
            double originLng,
            double destinationLat,
            double destinationLng,
            double distanceKm,
            int durationMin,
            BigDecimal estimatedPrice,
            BigDecimal taxRate,
            String vehicleCategory,
            String startPin,
            Integer driverArrivalMin) {
    }

    public record TripCancellationRequest(String reason) {
    }

    public record PendingReviewResponse(
            Integer tripId,
            String driverName,
            String driverPhotoUrl,
            String destinationAddress,
            BigDecimal finalPrice) {
    }

    public record TripReviewRequest(Integer rating, String comment) {
    }

    public record ReviewResponse(String message) {
    }

    public record TripCancellationResponse(
            Integer tripId,
            String status,
            String message) {
    }

    public record TripRequestResponse(
            Integer tripId,
            Integer routeId,
            double distanceKm,
            int durationMin,
            BigDecimal estimatedPrice,
            BigDecimal taxRate,
            String vehicleCategory,
            String tripType,
            LocalDateTime scheduledAt,
            String status) {
    }

    public record FareEstimateResponse(
            double distanceKm,
            int durationMin,
            BigDecimal netPrice,
            BigDecimal taxRate,
            BigDecimal taxAmount,
            BigDecimal estimatedPrice,
            Object geometry) {
    }
}
