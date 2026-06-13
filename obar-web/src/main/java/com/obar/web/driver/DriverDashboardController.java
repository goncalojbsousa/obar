package com.obar.web.driver;

import com.obar.bll.TripService;
import com.obar.bll.ReviewService;
import com.obar.bll.UserService;
import com.obar.bll.VehicleService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.Trip;
import com.obar.model.User;
import com.obar.model.Vehicle;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.UserType;
import com.obar.web.session.WebSessionHelper;
import com.obar.web.storage.SupabaseStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
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
    private final SupabaseStorageService storageService;
    private final ReviewService reviewService;

    public DriverDashboardController(TripService tripService, UserService userService, VehicleService vehicleService,
            SupabaseStorageService storageService, ReviewService reviewService) {
        this.tripService = tripService;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.storageService = storageService;
        this.reviewService = reviewService;
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
                tripService.findByClient(currentUser.id()),
                reviewService.countByReviewed(currentUser.id())));
        return "driver/profile";
    }

    @PostMapping("/driver/vehicles/{vehicleId}/photo")
    public String uploadVehiclePhoto(@PathVariable Integer vehicleId,
            @RequestParam("photo") MultipartFile photo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        AuthenticatedUserDto currentUser = requireDriver(session);
        Vehicle vehicle = vehicleService.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veiculo nao encontrado."));
        if (!vehicle.getDriver().getId().equals(currentUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nao podes alterar este veiculo.");
        }

        try {
            vehicle.setPhotoUrl(storageService.uploadVehiclePhoto(vehicle.getId(), photo));
            vehicleService.update(vehicle);
            redirectAttributes.addFlashAttribute("vehiclePhotoSuccess", "Foto do veiculo atualizada.");
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("vehiclePhotoError", exception.getReason());
        }
        return "redirect:/driver/profile";
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
            String photoUrl,
            String memberSince,
            String email,
            String phone,
            String taxNumber,
            String licenseNumber,
            boolean online,
            VehicleView vehicle,
            int completedTrips,
            String averageRating,
            int reviewCount,
            BigDecimal totalEarnings,
            WeeklyEarningsView weeklyEarnings,
            ActivityView lastDriverTrip,
            int clientTrips,
            int scheduledClientTrips,
            ActivityView lastClientTrip) {

        public static DriverProfileView from(User driver, List<Vehicle> vehicles, List<Trip> driverTrips,
                List<Trip> clientTrips, int reviewCount) {
            List<Trip> completedDriverTrips = driverTrips.stream()
                    .filter(trip -> trip.getStatus() == TripStatus.COMPLETED)
                    .toList();

            return new DriverProfileView(
                    valueOrFallback(driver.getName(), "Motorista"),
                    DriverDashboardController.initials(driver.getName()),
                    driver.getPhotoUrl(),
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
                    reviewCount,
                    completedDriverTrips.stream()
                            .map(trip -> trip.getFinalPrice() == null ? BigDecimal.ZERO : trip.getFinalPrice())
                            .reduce(BigDecimal.ZERO, BigDecimal::add),
                    WeeklyEarningsView.from(completedDriverTrips),
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

    public record WeeklyEarningsView(
            List<DailyEarningsView> days,
            BigDecimal total) {

        public static WeeklyEarningsView from(List<Trip> completedTrips) {
            LocalDate today = LocalDate.now();
            LocalDate firstDay = today.minusDays(6);

            List<DailyEarningsView> days = firstDay.datesUntil(today.plusDays(1))
                    .map(date -> new DailyEarningsView(
                            dayLabel(date),
                            date.format(DateTimeFormatter.ofPattern("dd MMM", PT_LOCALE)),
                            completedTrips.stream()
                                    .filter(trip -> tripReferenceDate(trip) != null)
                                    .filter(trip -> tripReferenceDate(trip).equals(date))
                                    .map(WeeklyEarningsView::tripEarnings)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add),
                            0))
                    .toList();

            BigDecimal maximum = days.stream()
                    .map(DailyEarningsView::amount)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            List<DailyEarningsView> scaledDays = days.stream()
                    .map(day -> day.withHeight(calculateHeight(day.amount(), maximum)))
                    .toList();

            return new WeeklyEarningsView(
                    scaledDays,
                    days.stream()
                            .map(DailyEarningsView::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        private static LocalDate tripReferenceDate(Trip trip) {
            LocalDateTime referenceTime = DriverProfileView.tripReferenceTime(trip);
            return referenceTime == null ? null : referenceTime.toLocalDate();
        }

        private static BigDecimal tripEarnings(Trip trip) {
            return trip.getFinalPrice() == null ? BigDecimal.ZERO : trip.getFinalPrice();
        }

        private static int calculateHeight(BigDecimal amount, BigDecimal maximum) {
            if (amount.signum() <= 0 || maximum.signum() <= 0) {
                return 0;
            }
            return Math.max(8, amount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(maximum, 0, RoundingMode.HALF_UP)
                    .intValue());
        }

        private static String dayLabel(LocalDate date) {
            String label = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_LOCALE);
            return label.substring(0, Math.min(3, label.length())).toUpperCase(PT_LOCALE);
        }
    }

    public record DailyEarningsView(
            String day,
            String date,
            BigDecimal amount,
            int heightPercent) {

        public DailyEarningsView withHeight(int height) {
            return new DailyEarningsView(day, date, amount, height);
        }
    }

    public record VehicleView(
            Integer id,
            String model,
            String photoUrl,
            String licensePlate,
            String category,
            String color,
            String year,
            boolean active) {

        public static VehicleView from(Vehicle vehicle) {
            return new VehicleView(
                    vehicle.getId(),
                    valueOrFallback(vehicle.getBrand(), "Marca") + " " + valueOrFallback(vehicle.getModel(), "Modelo"),
                    vehicle.getPhotoUrl(),
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
