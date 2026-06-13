package com.obar.web.client;

import com.obar.bll.TripService;
import com.obar.bll.ReviewService;
import com.obar.bll.UserService;
import com.obar.bll.VehicleService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.Trip;
import com.obar.model.User;
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

@Controller
public class ClientDashboardController {

    private final TripService tripService;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final SupabaseStorageService storageService;
    private final ReviewService reviewService;

    public ClientDashboardController(TripService tripService, UserService userService, VehicleService vehicleService,
            SupabaseStorageService storageService, ReviewService reviewService) {
        this.tripService = tripService;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.storageService = storageService;
        this.reviewService = reviewService;
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
                ClientViews.ClientProfileView.from(
                        client,
                        tripService.findByClient(currentUser.id()),
                        reviewService.countByReviewed(currentUser.id())));
        return "client/profile";
    }

    @PostMapping("/app/profile/photo")
    public String uploadProfilePhoto(@RequestParam("photo") MultipartFile photo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session).orElseThrow();
        User user = userService.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizador nao encontrado."));

        try {
            user.setPhotoUrl(storageService.uploadUserPhoto(user.getId(), photo));
            User updatedUser = userService.update(user);
            WebSessionHelper.login(session, AuthenticatedUserDto.from(updatedUser));
            redirectAttributes.addFlashAttribute("photoSuccess", "Foto de perfil atualizada.");
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("photoError", exception.getReason());
        }
        return currentUser.type() == UserType.DRIVER
                ? "redirect:/driver/profile"
                : "redirect:/app/profile";
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
