package com.obar.desktop.admin.sections.trips;

import com.obar.bll.admin.AdminTripDTO;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.DetailPanelBinder.DetailViewModel;

/**
 * Maps an {@link AdminTripDTO} to the {@link DetailViewModel} consumed by
 * {@link com.obar.desktop.admin.shared.DetailPanelBinder}.
 */
public final class TripDetailMapper {

    private TripDetailMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Builds a populated detail view model for the given trip.
     */
    public static DetailViewModel from(AdminTripDTO trip) {
        String route = AdminFormatUtils.fallback(trip.getOriginAddress())
                + " → " + AdminFormatUtils.fallback(trip.getDestinationAddress());

        return new DetailViewModel.Builder()
                .initials(trip.getId() == null ? "--" : "#" + trip.getId())
                .title("Detalhe da viagem")
                .name(route)
                .email("Cliente: " + AdminFormatUtils.fallback(trip.getClientName()))
                .status(AdminFormatUtils.prettyTripStatus(trip.getStatus()))
                .role(AdminFormatUtils.prettyTripType(trip.getTripType()))
                .phone("Motorista: " + AdminFormatUtils.fallback(trip.getDriverName()))
                .created(trip.getRequestTime() == null ? "-" : trip.getRequestTime().toString())
                .card1("ID Cliente", trip.getClientId() == null ? "-" : "#" + trip.getClientId())
                .card2("ID Motorista", trip.getDriverId() == null ? "-" : "#" + trip.getDriverId())
                .card3("Veiculo", AdminFormatUtils.fallback(trip.getVehicleDisplay()))
                .card4("Distancia", trip.getDistanceKm() == null ? "-" : trip.getDistanceKm() + " km")
                .ref("Preco estimado", trip.getEstimatedPrice() == null ? "-" : "EUR " + trip.getEstimatedPrice())
                .extra1("Preco final", trip.getFinalPrice() == null ? "-" : "EUR " + trip.getFinalPrice())
                .extra2("Inicio", trip.getStartTime() == null ? "-" : trip.getStartTime().toString())
                .extra3("Fim", trip.getEndTime() == null ? "-" : trip.getEndTime().toString())
                .build();
    }

    /**
     * Builds the empty/placeholder state shown when nothing is selected.
     */
    public static DetailViewModel empty() {
        return new DetailViewModel.Builder()
                .initials("--")
                .title("Sem selecao")
                .name("Selecione uma viagem")
                .card1("ID Cliente", "-")
                .card2("ID Motorista", "-")
                .card3("Veiculo", "-")
                .card4("Distancia", "-")
                .ref("Preco estimado", "-")
                .extra1("Preco final", "-")
                .extra2("Inicio", "-")
                .extra3("Fim", "-")
                .build();
    }
}
