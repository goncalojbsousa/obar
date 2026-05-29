package com.obar.web.maps;

import com.obar.bll.RouteService;
import com.obar.bll.TripService;
import com.obar.bll.UserService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.Route;
import com.obar.model.Trip;
import com.obar.model.User;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import com.obar.web.maps.client.MapsServiceClient;
import com.obar.web.maps.dto.request.RouteEstimateRequest;
import com.obar.web.maps.dto.response.ActiveTripResponse;
import com.obar.web.maps.dto.response.LocationSuggestionResponse;
import com.obar.web.maps.dto.response.RouteEstimateResponse;
import com.obar.web.maps.dto.response.TripCancellationResponse;
import com.obar.web.maps.dto.response.TripRequestResponse;
import com.obar.web.maps.utils.VehicleCategoryCatalog;
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

import java.util.Comparator;
import java.util.List;
import java.time.LocalDateTime;

@RestController
public class MapsApiController {

    private final MapsServiceClient mapsServiceClient;
    private final RouteService routeService;
    private final TripService tripService;
    private final UserService userService;

    public MapsApiController(
            MapsServiceClient mapsServiceClient,
            RouteService routeService,
            TripService tripService,
            UserService userService) {
        this.mapsServiceClient = mapsServiceClient;
        this.routeService = routeService;
        this.tripService = tripService;
        this.userService = userService;
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
    public RouteEstimateResponse estimateRoute(@RequestBody RouteEstimateRequest request) {
        return mapsServiceClient.estimate(request);
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
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        User client = userService.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (tripType == TripType.IMMEDIATE && hasActiveTrip(client.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já tens uma viagem ativa ou à espera de motorista.");
        }
        LocalDateTime scheduledAt = tripType == TripType.SCHEDULED ? validateScheduledAt(request.scheduledAt()) : null;

        String vehicleCategory = VehicleCategoryCatalog.normalize(request.vehicleCategory());
        RouteEstimateResponse estimate = mapsServiceClient.estimate(request);

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
        trip.setEstimatedPrice(estimate.estimatedPrice());
        Trip savedTrip = tripService.requestTrip(trip);

        return new TripRequestResponse(
                savedTrip.getId(),
                savedRoute.getId(),
                estimate.distanceKm(),
                estimate.durationMin(),
                estimate.estimatedPrice(),
                savedTrip.getVehicleCategory(),
                savedTrip.getTripType().name(),
                savedTrip.getScheduledTime(),
                savedTrip.getStatus().name());
    }

    @GetMapping("/api/trips/active")
    public ActiveTripResponse activeTrip(HttpSession session) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return tripService.findByClient(currentUser.id()).stream()
                .filter(this::isActiveTrip)
                .max(Comparator.comparing(Trip::getRequestTime))
                .map(this::toActiveTripResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não existe viagem ativa."));
    }

    @PostMapping("/api/trips/{tripId}/cancel")
    public TripCancellationResponse cancelTrip(@PathVariable Integer tripId, HttpSession session) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Trip trip = tripService.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viagem não encontrada."));

        if (!trip.getClient().getId().equals(currentUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não podes cancelar esta viagem.");
        }

        if (trip.getStatus() != TripStatus.PENDING && trip.getStatus() != TripStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta viagem já não pode ser cancelada.");
        }

        Trip cancelledTrip = tripService.cancelTrip(tripId, "CLIENT",
                "Cancelado pelo cliente enquanto aguardava motorista.");
        return new TripCancellationResponse(
                cancelledTrip.getId(),
                cancelledTrip.getStatus().name(),
                "Pedido de viagem cancelado.");
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean hasActiveTrip(Integer clientId) {
        return tripService.findByClient(clientId).stream()
                .anyMatch(this::isActiveTrip);
    }

    private boolean isActiveTrip(Trip trip) {
        return trip.getTripType() == TripType.IMMEDIATE
                && (trip.getStatus() == TripStatus.PENDING
                        || trip.getStatus() == TripStatus.ACCEPTED
                        || trip.getStatus() == TripStatus.IN_PROGRESS);
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
                trip.getVehicleCategory());
    }

    private LocalDateTime validateScheduledAt(LocalDateTime scheduledAt) {
        if (scheduledAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolhe a data e hora da viagem.");
        }
        if (scheduledAt.isBefore(LocalDateTime.now().plusMinutes(15))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A viagem agendada deve ser marcada com pelo menos 15 minutos de antecedência.");
        }
        return scheduledAt;
    }

}
