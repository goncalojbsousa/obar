package com.obar.web.maps;

import com.obar.bll.TripService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "obar.trip-dispatch.enabled", havingValue = "true", matchIfMissing = true)
public class TripDispatchScheduler {

    private final TripService tripService;

    public TripDispatchScheduler(TripService tripService) {
        this.tripService = tripService;
    }

    @Scheduled(fixedDelayString = "${obar.trip-dispatch.expiration-check-ms:5000}")
    public void expireTimedOutDriverAssignments() {
        tripService.expireTimedOutAssignmentsAndDispatchNext();
    }
}
