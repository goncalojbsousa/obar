package com.obar.web.driver;

import com.obar.model.Trip;
import com.obar.model.enums.TripStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DriverHistoryViewsTest {

    @Test
    void paginatesTripsFromMostRecentAndCalculatesCompletedTotals() {
        Trip older = trip(1, LocalDateTime.of(2026, 6, 10, 10, 0), TripStatus.COMPLETED, "12.50");
        Trip recent = trip(2, LocalDateTime.of(2026, 6, 14, 12, 0), TripStatus.ACCEPTED, "18.00");
        Trip completed = trip(3, LocalDateTime.of(2026, 6, 12, 9, 0), TripStatus.COMPLETED, "7.25");

        DriverHistoryViews.HistoryPage page = DriverHistoryViews.HistoryPage.from(
                List.of(recent, completed, older),
                1,
                1,
                3,
                2,
                new BigDecimal("4.94"),
                "",
                null,
                null,
                "");

        assertThat(page.trips()).extracting(DriverHistoryViews.HistoryTrip::id)
                .containsExactly(2, 3, 1);
        assertThat(page.totalTrips()).isEqualTo(3);
        assertThat(page.completedTrips()).isEqualTo(2);
        assertThat(page.totalEarnings()).isEqualByComparingTo("4.94");
        assertThat(page.hasPagination()).isFalse();
    }

    private Trip trip(int id, LocalDateTime requestTime, TripStatus status, String price) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setRequestTime(requestTime);
        trip.setStatus(status);
        trip.setFinalPrice(new BigDecimal(price));
        return trip;
    }
}
