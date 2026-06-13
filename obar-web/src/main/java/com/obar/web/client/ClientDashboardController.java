package com.obar.web.client;

import com.obar.bll.TripService;
import com.obar.bll.UserService;
import com.obar.bll.VehicleService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.Trip;
import com.obar.model.User;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.UserType;
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
    private final UserService userService;
    private final VehicleService vehicleService;

    public ClientDashboardController(TripService tripService, UserService userService, VehicleService vehicleService) {
        this.tripService = tripService;
        this.userService = userService;
        this.vehicleService = vehicleService;
    }

    @GetMapping("/app")
    public String dashboard(HttpSession session, Model model) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session).orElseThrow();
        if (currentUser.type() == UserType.DRIVER
                && userService.findById(currentUser.id())
                        .map(User::getOnline)
                        .orElse(false)) {
            return "redirect:/driver";
        }
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("vehicleCategories", vehicleService.supportedCategories());
        return "client/dashboard";
    }

    @GetMapping("/app/scheduled")
    public String scheduledTrips(@RequestParam(name = "recentPage", defaultValue = "1") int recentPage,
            HttpSession session,
            Model model) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session).orElseThrow();
        var scheduledTrips = tripService.findScheduledByClient(currentUser.id());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("scheduledPage",
                ClientViews.ScheduledTripsPageView.from(
                        scheduledTrips,
                        tripService.findByClient(currentUser.id()),
                        recentPage));
        return "client/scheduled";
    }

    @GetMapping("/app/profile")
    public String profile(HttpSession session, Model model) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session).orElseThrow();
        User client = userService.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente nao encontrado."));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("clientProfile",
                ClientViews.ClientProfileView.from(client, tripService.findByClient(currentUser.id())));
        return "client/profile";
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
