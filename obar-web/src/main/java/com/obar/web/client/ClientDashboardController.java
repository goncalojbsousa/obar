package com.obar.web.client;

import com.obar.bll.TripService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.Trip;
import com.obar.model.enums.TripStatus;
import com.obar.web.maps.utils.VehicleCategoryCatalog;
import com.obar.web.session.WebSessionHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ClientDashboardController {

    private final TripService tripService;

    public ClientDashboardController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/app")
    public String dashboard(HttpSession session, Model model) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session).orElseThrow();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("vehicleCategories", VehicleCategoryCatalog.supported());
        return "client/dashboard";
    }

    @GetMapping("/app/scheduled")
    public String scheduledTrips(HttpSession session, Model model) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session).orElseThrow();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("scheduledTrips", tripService.findScheduledByClient(currentUser.id()).stream()
                .map(ScheduledTripView::from)
                .toList());
        return "client/scheduled";
    }

    @PostMapping("/app/scheduled/{tripId}/cancel")
    public String cancelScheduledTrip(@PathVariable Integer tripId,
            @RequestParam("reason") String reason,
            HttpSession session) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session).orElseThrow();
        Trip trip = tripService.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viagem não encontrada."));
        if (!trip.getClient().getId().equals(currentUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não podes cancelar esta viagem.");
        }
        if (trip.getStatus() != TripStatus.PENDING && trip.getStatus() != TripStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta viagem já não pode ser cancelada.");
        }
        tripService.cancelTrip(tripId, "CLIENT", reason);
        return "redirect:/app/scheduled";
    }
}
