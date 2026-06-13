package com.obar.web.driver;

import com.obar.bll.TripService;
import com.obar.bll.UserService;
import com.obar.bll.VehicleService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.Trip;
import com.obar.model.User;
import com.obar.model.Vehicle;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.UserType;
import com.obar.web.session.WebSessionHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Controller
public class DriverDashboardController {

    private static final Locale PT_LOCALE = Locale.forLanguageTag("pt-PT");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", PT_LOCALE);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", PT_LOCALE);

    private final TripService tripService;
    private final UserService userService;
    private final VehicleService vehicleService;

    public DriverDashboardController(TripService tripService, UserService userService, VehicleService vehicleService) {
        this.tripService = tripService;
        this.userService = userService;
        this.vehicleService = vehicleService;
    }

    @GetMapping("/driver")
    public String dashboard(HttpSession session, Model model) {
        model.addAttribute("currentUser", requireDriver(session));
        return "driver/dashboard";
    }

    @GetMapping("/driver/profile")
    public String profile(HttpSession session, Model model) {
        AuthenticatedUserDto currentUser = requireDriver(session);
        User driver = userService.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Motorista nao encontrado."));

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("driverProfile", DriverProfileView.from(
                driver,
                vehicleService.findByDriver(currentUser.id()),
                tripService.findByDriver(currentUser.id()),
                tripService.findByClient(currentUser.id())));
        return "driver/profile";
    }

    private AuthenticatedUserDto requireDriver(HttpSession session) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session).orElseThrow();
        if (currentUser.type() != UserType.DRIVER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "\u00C1rea reservada a motoristas.");
        }
        return currentUser;
    }

    public record DriverProfileView(
            String name,
            String initials,
            String memberSince,
            String email,
            String phone,
            String taxNumber,
            String licenseNumber,
            boolean online,
            VehicleView vehicle,
            int completedTrips,
            String averageRating,
            BigDecimal totalEarnings,
            ActivityView lastDriverTrip,
            int clientTrips,
            int scheduledClientTrips,
            ActivityView lastClientTrip) {

        public static DriverProfileView from(User driver, List<Vehicle> vehicles, List<Trip> driverTrips,
                List<Trip> clientTrips) {
            List<Trip> completedDriverTrips = driverTrips.stream()
                    .filter(trip -> trip.getStatus() == TripStatus.COMPLETED)
                    .toList();

            return new DriverProfileView(
                    valueOrFallback(driver.getName(), "Motorista"),
                    DriverDashboardController.initials(driver.getName()),
                    formatMonth(driver.getCreatedAt()),
                    valueOrFallback(driver.getEmail(), "-"),
                    valueOrFallback(driver.getPhone(), "-"),
                    valueOrFallback(driver.getTaxNumber(), "-"),
                    valueOrFallback(driver.getLicenseNumber(), "-"),
                    Boolean.TRUE.equals(driver.getOnline()),
                    vehicles.stream()
                            .sorted(Comparator.comparing((Vehicle vehicle) -> Boolean.TRUE.equals(vehicle.getActive()))
                                    .reversed())
                            .findFirst()
                            .map(VehicleView::from)
                            .orElse(null),
                    driver.getTotalTrips() == null ? completedDriverTrips.size() : driver.getTotalTrips(),
                    String.format(PT_LOCALE, "%.1f", driver.getAverageRating() == null ? 0f : driver.getAverageRating()),
                    completedDriverTrips.stream()
                            .map(trip -> trip.getFinalPrice() == null ? BigDecimal.ZERO : trip.getFinalPrice())
                            .reduce(BigDecimal.ZERO, BigDecimal::add),
                    driverTrips.stream()
                            .max(Comparator.comparing(DriverProfileView::tripReferenceTime,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(ActivityView::from)
                            .orElse(ActivityView.empty("Ainda sem viagens como motorista")),
                    clientTrips.size(),
                    (int) clientTrips.stream().filter(DriverProfileView::isScheduledClientTrip).count(),
                    clientTrips.stream()
                            .max(Comparator.comparing(DriverProfileView::tripReferenceTime,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(ActivityView::from)
                            .orElse(ActivityView.empty("Ainda sem viagens como cliente")));
        }

        public String onlineLabel() {
            return online ? "Online" : "Offline";
        }

        private static boolean isScheduledClientTrip(Trip trip) {
            return trip.getScheduledTime() != null
                    && trip.getStatus() != TripStatus.CANCELLED
                    && trip.getStatus() != TripStatus.COMPLETED;
        }

        private static LocalDateTime tripReferenceTime(Trip trip) {
            if (trip.getEndTime() != null) {
                return trip.getEndTime();
            }
            return trip.getScheduledTime() == null ? trip.getRequestTime() : trip.getScheduledTime();
        }
    }

    public record VehicleView(
            String model,
            String licensePlate,
            String category,
            String color,
            String year,
            boolean active) {

        public static VehicleView from(Vehicle vehicle) {
            return new VehicleView(
                    valueOrFallback(vehicle.getBrand(), "Marca") + " " + valueOrFallback(vehicle.getModel(), "Modelo"),
                    valueOrFallback(vehicle.getLicensePlate(), "-"),
                    valueOrFallback(vehicle.getCategory(), "STANDARD"),
                    valueOrFallback(vehicle.getColor(), "-"),
                    vehicle.getYear() == null ? "-" : vehicle.getYear().toString(),
                    Boolean.TRUE.equals(vehicle.getActive()));
        }

        public String statusLabel() {
            return active ? "Ativo" : "Inativo";
        }
    }

    public record ActivityView(
            String date,
            String route,
            String status,
            BigDecimal price) {

        public static ActivityView from(Trip trip) {
            return new ActivityView(
                    formatDate(DriverProfileView.tripReferenceTime(trip)),
                    routeLabel(trip),
                    statusLabel(trip.getStatus()),
                    trip.getFinalPrice() == null
                            ? trip.getEstimatedPrice() == null ? BigDecimal.ZERO : trip.getEstimatedPrice()
                            : trip.getFinalPrice());
        }

        public static ActivityView empty(String route) {
            return new ActivityView("-", route, "-", BigDecimal.ZERO);
        }
    }

    private static String routeLabel(Trip trip) {
        if (trip.getRoute() == null) {
            return "Rota indisponivel";
        }
        return valueOrFallback(trip.getRoute().getOriginAddress(), "Origem")
                + " -> "
                + valueOrFallback(trip.getRoute().getDestinationAddress(), "Destino");
    }

    private static String statusLabel(TripStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case PENDING -> "Pendente";
            case ACCEPTED -> "Aceite";
            case IN_PROGRESS -> "Em curso";
            case COMPLETED -> "Conclu\u00EDda";
            case CANCELLED -> "Cancelada";
            case REJECTED -> "Rejeitada";
        };
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "M";
        }
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase(Locale.ROOT);
    }

    private static String formatMonth(LocalDateTime value) {
        return value == null ? "-" : MONTH_FORMATTER.format(value);
    }

    private static String formatDate(LocalDateTime value) {
        return value == null ? "-" : DATE_FORMATTER.format(value);
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
