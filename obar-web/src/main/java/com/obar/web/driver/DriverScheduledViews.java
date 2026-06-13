package com.obar.web.driver;

import com.obar.model.Trip;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class DriverScheduledViews {

    private DriverScheduledViews() {
    }

    public record ScheduledTripsPage(
            List<ScheduledTrip> availableTrips,
            List<ScheduledTrip> acceptedTrips) {

        public static ScheduledTripsPage from(List<Trip> availableTrips, List<Trip> acceptedTrips) {
            return new ScheduledTripsPage(
                    availableTrips.stream().map(ScheduledTrip::from).toList(),
                    acceptedTrips.stream().map(ScheduledTrip::from).toList());
        }
    }

    public record ScheduledTrip(
            Integer id,
            String clientName,
            Float clientRating,
            String originAddress,
            String destinationAddress,
            String vehicleCategory,
            LocalDateTime scheduledTime,
            BigDecimal estimatedPrice,
            Float distanceKm,
            Integer durationMin) {

        private static final Locale PT_LOCALE = Locale.forLanguageTag("pt-PT");
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy", PT_LOCALE);
        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

        public static ScheduledTrip from(Trip trip) {
            return new ScheduledTrip(
                    trip.getId(),
                    trip.getClient().getName(),
                    trip.getClient().getAverageRating(),
                    trip.getRoute().getOriginAddress(),
                    trip.getRoute().getDestinationAddress(),
                    trip.getVehicleCategory(),
                    trip.getScheduledTime(),
                    trip.getEstimatedPrice() == null ? BigDecimal.ZERO : trip.getEstimatedPrice(),
                    trip.getRoute().getDistanceKm(),
                    trip.getRoute().getEstimatedDurationMin());
        }

        public String dayLabel() {
            if (scheduledTime == null) {
                return "Data por confirmar";
            }
            LocalDate date = scheduledTime.toLocalDate();
            if (date.equals(LocalDate.now())) {
                return "Hoje";
            }
            if (date.equals(LocalDate.now().plusDays(1))) {
                return "Amanh\u00E3";
            }
            return DATE_FORMATTER.format(date);
        }

        public String timeLabel() {
            return scheduledTime == null ? "-" : TIME_FORMATTER.format(scheduledTime);
        }

        public String distanceLabel() {
            return distanceKm == null ? "-" : String.format(PT_LOCALE, "%.1f km", distanceKm);
        }

        public String durationLabel() {
            return durationMin == null ? "-" : durationMin + " min";
        }

        public String ratingLabel() {
            return clientRating == null || clientRating <= 0
                    ? "Sem avalia\u00E7\u00F5es"
                    : String.format(PT_LOCALE, "%.1f", clientRating);
        }
    }
}
