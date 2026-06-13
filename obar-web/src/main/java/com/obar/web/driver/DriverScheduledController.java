package com.obar.web.driver;

import com.obar.bll.TripService;
import com.obar.bll.UserService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.User;
import com.obar.model.enums.UserType;
import com.obar.web.session.WebSessionHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DriverScheduledController {

    private final TripService tripService;
    private final UserService userService;

    public DriverScheduledController(TripService tripService, UserService userService) {
        this.tripService = tripService;
        this.userService = userService;
    }

    @GetMapping("/driver/scheduled")
    public String scheduledTrips(HttpSession session, Model model) {
        AuthenticatedUserDto currentUser = requireDriver(session);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("scheduledPage", DriverScheduledViews.ScheduledTripsPage.from(
                tripService.findPendingScheduledForDriver(currentUser.id()),
                tripService.findAcceptedScheduledForDriver(currentUser.id())));
        return "driver/scheduled";
    }

    @PostMapping("/driver/scheduled/{tripId}/accept")
    public String acceptScheduledTrip(
            @PathVariable Integer tripId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        AuthenticatedUserDto currentUser = requireDriver(session);
        User driver = userService.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        try {
            tripService.acceptScheduledTrip(tripId, driver);
            redirectAttributes.addFlashAttribute("successMessage", "Viagem programada aceite.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/driver/scheduled";
    }

    private AuthenticatedUserDto requireDriver(HttpSession session) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (currentUser.type() != UserType.DRIVER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "\u00C1rea reservada a motoristas.");
        }
        return currentUser;
    }
}
