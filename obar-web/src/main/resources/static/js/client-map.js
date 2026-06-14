const defaultCenter = [38.7223, -9.1393];

const state = {
    origin: null,
    destination: null,
    routeLayer: null,
    originMarker: null,
    destinationMarker: null,
    suggestionsTimers: {
        origin: null,
        destination: null
    },
    activeTripId: null,
    activeTripPoll: null,
    tripLocked: false,
    pendingReviewTripId: null
};

const map = L.map("ride-map", { zoomControl: true }).setView(defaultCenter, 13);
map.createPane("routePane");
map.getPane("routePane").style.zIndex = 450;
map.getPane("routePane").style.pointerEvents = "none";
map.createPane("tripMarkerPane");
map.getPane("tripMarkerPane").style.zIndex = 720;

L.tileLayer("https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png", {
    maxZoom: 20,
    subdomains: "abcd",
    attribution: "&copy; OpenStreetMap contributors &copy; CARTO"
}).addTo(map);

const elements = {
    rideForm: document.querySelector("[data-ride-form]"),
    originStatus: document.querySelector("[data-origin-status]"),
    originInput: document.querySelector("[data-origin-input]"),
    originSuggestions: document.querySelector("[data-origin-suggestions]"),
    locateButton: document.querySelector("[data-locate-button]"),
    destinationInput: document.querySelector("[data-destination-input]"),
    destinationSuggestions: document.querySelector("[data-location-suggestions]"),
    vehicleCategory: document.querySelector("[data-vehicle-category]"),
    notes: document.querySelector("[data-trip-notes]"),
    scheduledAt: document.querySelector("[data-scheduled-at]"),
    requestButton: document.querySelector("[data-request-button]"),
    scheduleButton: document.querySelector("[data-schedule-button]"),
    estimatePanel: document.querySelector("[data-estimate-panel]"),
    distance: document.querySelector("[data-distance]"),
    duration: document.querySelector("[data-duration]"),
    price: document.querySelector("[data-price]"),
    taxRate: document.querySelector("[data-tax-rate]"),
    message: document.querySelector("[data-ride-message]"),
    waitingPanel: document.querySelector("[data-waiting-panel]"),
    summaryOrigin: document.querySelector("[data-summary-origin]"),
    summaryDestination: document.querySelector("[data-summary-destination]"),
    summaryCategory: document.querySelector("[data-summary-category]"),
    summaryDistance: document.querySelector("[data-summary-distance]"),
    summaryDuration: document.querySelector("[data-summary-duration]"),
    summaryPrice: document.querySelector("[data-summary-price]"),
    summaryNotesBlock: document.querySelector("[data-summary-notes-block]"),
    summaryNotes: document.querySelector("[data-summary-notes]"),
    driverDetails: document.querySelector("[data-driver-details]"),
    driverAvatar: document.querySelector("[data-driver-avatar]"),
    driverName: document.querySelector("[data-driver-name]"),
    vehicleName: document.querySelector("[data-vehicle-name]"),
    vehicleLicensePlate: document.querySelector("[data-vehicle-license-plate]"),
    driverArrivalBlock: document.querySelector("[data-driver-arrival-block]"),
    driverArrival: document.querySelector("[data-driver-arrival]"),
    tripPinBlock: document.querySelector("[data-trip-pin-block]"),
    tripPin: document.querySelector("[data-trip-pin]"),
    cancelTripButton: document.querySelector("[data-cancel-trip-button]"),
    cancelModal: document.querySelector("[data-cancel-modal]"),
    cancelForm: document.querySelector("[data-cancel-form]"),
    cancelReason: document.querySelector("[data-cancel-reason]"),
    cancelError: document.querySelector("[data-cancel-error]"),
    cancelSubmitButton: document.querySelector("[data-cancel-submit]"),
    cancelDismissButtons: document.querySelectorAll("[data-cancel-dismiss]"),
    reviewModal: document.querySelector("[data-review-modal]"),
    reviewForm: document.querySelector("[data-review-form]"),
    reviewDriverAvatar: document.querySelector("[data-review-driver-avatar]"),
    reviewDriverName: document.querySelector("[data-review-driver-name]"),
    reviewDestination: document.querySelector("[data-review-destination]"),
    reviewPrice: document.querySelector("[data-review-price]"),
    reviewComment: document.querySelector("[data-review-comment]"),
    reviewError: document.querySelector("[data-review-error]"),
    reviewSubmitButton: document.querySelector("[data-review-submit]"),
    reviewDismissButtons: document.querySelectorAll("[data-review-dismiss]")
};

const routeInputs = [
    elements.destinationInput,
    elements.originInput,
    elements.vehicleCategory,
    elements.notes,
    elements.scheduledAt
];
const tripButtons = [elements.requestButton, elements.scheduleButton];

elements.locateButton.addEventListener("click", locateUser);
elements.originInput.addEventListener("input", (event) => handleLocationInput("origin", event));
elements.destinationInput.addEventListener("input", (event) => handleLocationInput("destination", event));
elements.vehicleCategory.addEventListener("change", () => state.origin && state.destination && estimateRoute());
elements.scheduledAt.addEventListener("click", openSchedulePicker);
elements.requestButton.addEventListener("click", requestTrip);
elements.scheduleButton.addEventListener("click", scheduleTrip);
elements.cancelTripButton.addEventListener("click", openCancelModal);
elements.cancelForm.addEventListener("submit", submitCancelTrip);
elements.cancelDismissButtons.forEach((button) => button.addEventListener("click", closeCancelModal));
elements.reviewForm.addEventListener("submit", submitDriverReview);
elements.reviewDismissButtons.forEach((button) => button.addEventListener("click", closeReviewModal));
document.addEventListener("keydown", handleModalKeydown);

initializeScheduleInput();
initializeRide();

async function initializeRide() {
    try {
        const activeTrip = await fetchJson("/api/trips/active");
        await renderActiveTrip(activeTrip);
    } catch (error) {
        if (error.status === 404) {
            await loadPendingReview();
            locateUser({ automatic: true });
            return;
        }
        setMessage(error.message);
    }
}

function locateUser(options = {}) {
    const automatic = options.automatic === true;
    setMessage("");
    elements.locateButton.disabled = true;
    elements.locateButton.hidden = automatic;
    elements.originStatus.textContent = "A obter localização...";

    if (!navigator.geolocation) {
        elements.originStatus.textContent = "Geolocalização indisponível neste browser.";
        elements.locateButton.disabled = false;
        elements.locateButton.hidden = false;
        return;
    }

    navigator.geolocation.getCurrentPosition(
        async (position) => {
            const currentPoint = {
                lat: position.coords.latitude,
                lng: position.coords.longitude,
                label: "Localização atual"
            };

            state.origin = currentPoint;
            elements.originInput.value = "A identificar morada...";
            elements.originStatus.textContent = "Localização atual encontrada.";
            setMarker("origin", state.origin, "Origem");
            map.setView([state.origin.lat, state.origin.lng], 15);

            try {
                const location = await fetchJson(`/api/locations/reverse?lat=${state.origin.lat}&lng=${state.origin.lng}`);
                state.origin = location;
                elements.originInput.value = location.label;
                setMarker("origin", state.origin, "Origem");
            } catch {
                elements.originInput.value = "Localização atual";
            }

            elements.locateButton.disabled = false;
            elements.locateButton.hidden = true;
            if (state.destination) {
                estimateRoute();
            }
        },
        () => {
            elements.originStatus.textContent = automatic
                ? "Não foi possível obter a localização automaticamente."
                : "Permissão negada ou localização indisponível.";
            elements.originInput.placeholder = "Rua, cidade ou ponto de interesse";
            elements.locateButton.disabled = false;
            elements.locateButton.hidden = false;
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    );
}

function handleLocationInput(type, event) {
    if (state.tripLocked) {
        return;
    }

    const text = event.target.value.trim();
    state[type] = null;
    clearRouteEstimate();

    window.clearTimeout(state.suggestionsTimers[type]);
    if (text.length < 3) {
        renderSuggestions(type, []);
        return;
    }

    state.suggestionsTimers[type] = window.setTimeout(async () => {
        try {
            setMessage(type === "origin" ? "A procurar origens..." : "A procurar destinos...");
            const suggestions = await fetchJson(`/api/locations/search?text=${encodeURIComponent(text)}`);
            renderSuggestions(type, suggestions);
            setMessage(suggestions.length === 0 ? "Não foram encontrados locais para essa pesquisa." : "");
        } catch (error) {
            setMessage(error.message);
            renderSuggestions(type, []);
        }
    }, 300);
}

function renderSuggestions(type, suggestions) {
    const list = type === "origin" ? elements.originSuggestions : elements.destinationSuggestions;
    list.innerHTML = "";
    list.hidden = suggestions.length === 0;

    suggestions.forEach((suggestion) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "suggestion-option";
        button.textContent = suggestion.label;
        button.addEventListener("click", () => selectLocation(type, suggestion));
        list.appendChild(button);
    });
}

function selectLocation(type, suggestion) {
    state[type] = suggestion;
    const input = type === "origin" ? elements.originInput : elements.destinationInput;
    input.value = suggestion.label;
    renderSuggestions(type, []);
    setMarker(type, suggestion, type === "origin" ? "Origem" : "Destino");
    map.setView([suggestion.lat, suggestion.lng], 15);
    clearRouteEstimate();

    if (state.origin && state.destination) {
        estimateRoute();
    } else if (type === "destination") {
        setMessage("Destino selecionado. Define a origem para calcular a rota.");
    } else {
        setMessage("Origem selecionada. Escolhe um destino para calcular a rota.");
    }
}

async function estimateRoute() {
    if (!state.origin || !state.destination || state.tripLocked) {
        return;
    }

    setBusy(true);
    setMessage("A calcular rota...");
    try {
        const estimate = await fetchJson("/api/routes/estimate", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(buildRouteRequest())
        });
        renderEstimate(estimate);
        setMessage("");
    } catch (error) {
        setMessage(error.message);
    } finally {
        setBusy(false);
    }
}

async function requestTrip() {
    if (!state.origin || !state.destination || state.tripLocked) {
        return;
    }

    setBusy(true);
    setMessage("");
    try {
        const trip = await fetchJson("/api/trips/request", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(buildRouteRequest())
        });
        state.activeTripId = trip.tripId;
        renderTripSummary(trip);
        setTripLockedState(trip.status);
        renderTripPin(trip);
        setMessage("");
    } catch (error) {
        setMessage(error.message);
    } finally {
        setBusy(false);
    }
}

async function scheduleTrip() {
    if (!state.origin || !state.destination || state.tripLocked) {
        return;
    }

    if (!elements.scheduledAt.value) {
        setMessage("Escolhe a data e hora para agendar a viagem.");
        openSchedulePicker();
        return;
    }

    setBusy(true);
    setMessage("");
    try {
        const trip = await fetchJson("/api/trips/schedule", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(buildRouteRequest(true))
        });
        setMessage(`Viagem agendada para ${formatDateTime(trip.scheduledAt)}. Podes continuar a pedir viagens imediatas.`);
    } catch (error) {
        setMessage(error.message);
    } finally {
        setBusy(false);
    }
}

function openCancelModal() {
    if (!state.activeTripId) {
        return;
    }

    elements.cancelReason.value = "";
    elements.cancelError.hidden = true;
    elements.cancelSubmitButton.disabled = false;
    elements.cancelModal.hidden = false;
    window.setTimeout(() => elements.cancelReason.focus(), 0);
}

function closeCancelModal() {
    if (elements.cancelSubmitButton.disabled) {
        return;
    }

    elements.cancelModal.hidden = true;
    elements.cancelError.hidden = true;
}

async function submitCancelTrip(event) {
    event.preventDefault();
    if (!state.activeTripId) {
        closeCancelModal();
        return;
    }

    const reason = elements.cancelReason.value.trim();
    if (reason.length < 3) {
        elements.cancelError.textContent = "Indica um motivo com pelo menos 3 caracteres.";
        elements.cancelError.hidden = false;
        elements.cancelReason.focus();
        return;
    }

    elements.cancelSubmitButton.disabled = true;
    setBusy(true);
    elements.cancelTripButton.disabled = true;
    elements.cancelError.hidden = true;
    setMessage("A cancelar viagem...");
    try {
        await fetchJson(`/api/trips/${state.activeTripId}/cancel`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ reason })
        });
        elements.cancelModal.hidden = true;
        state.activeTripId = null;
        setTripLockedState(null);
        stopActiveTripPolling();
        setMessage("Viagem cancelada. Podes escolher outro destino ou pedir novamente.");
    } catch (error) {
        elements.cancelError.textContent = error.message;
        elements.cancelError.hidden = false;
        elements.cancelSubmitButton.disabled = false;
        elements.cancelTripButton.disabled = false;
    } finally {
        setBusy(false);
    }
}

function handleModalKeydown(event) {
    if (event.key === "Escape" && !elements.cancelModal.hidden) {
        closeCancelModal();
    } else if (event.key === "Escape" && !elements.reviewModal.hidden) {
        closeReviewModal();
    }
}

function openSchedulePicker() {
    if (elements.scheduledAt.disabled) {
        return;
    }

    elements.scheduledAt.focus();
    if (typeof elements.scheduledAt.showPicker === "function") {
        try {
            elements.scheduledAt.showPicker();
        } catch {
            // The native field remains usable when the browser blocks showPicker.
        }
    }
}

async function loadPendingReview() {
    try {
        const review = await fetchJson("/api/trips/review-pending");
        openReviewModal(review);
    } catch (error) {
        if (error.status !== 404) {
            setMessage(error.message);
        }
    }
}

function openReviewModal(review) {
    state.pendingReviewTripId = review.tripId;
    elements.reviewForm.reset();
    elements.reviewError.hidden = true;
    elements.reviewSubmitButton.disabled = false;
    elements.reviewDriverName.textContent = review.driverName;
    elements.reviewDestination.textContent = review.destinationAddress || "Viagem concluída";
    elements.reviewPrice.textContent = formatCurrency(review.finalPrice || 0);
    const hasDriverPhoto = Boolean(review.driverPhotoUrl);
    elements.reviewDriverAvatar.textContent = hasDriverPhoto
        ? ""
        : review.driverName
            ? review.driverName.trim().substring(0, 1).toUpperCase()
            : "M";
    elements.reviewDriverAvatar.style.backgroundImage = hasDriverPhoto
        ? `url("${review.driverPhotoUrl.replaceAll('"', "%22")}")`
        : "";
    elements.reviewModal.hidden = false;
}

function closeReviewModal() {
    if (elements.reviewSubmitButton.disabled) {
        return;
    }
    elements.reviewModal.hidden = true;
    elements.reviewError.hidden = true;
}

async function submitDriverReview(event) {
    event.preventDefault();
    const ratingInput = elements.reviewForm.querySelector('input[name="rating"]:checked');
    if (!ratingInput) {
        elements.reviewError.textContent = "Escolhe entre 1 e 5 estrelas.";
        elements.reviewError.hidden = false;
        return;
    }
    if (!state.pendingReviewTripId) {
        elements.reviewModal.hidden = true;
        return;
    }

    elements.reviewSubmitButton.disabled = true;
    elements.reviewError.hidden = true;
    try {
        const result = await fetchJson(`/api/trips/${state.pendingReviewTripId}/review`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                rating: Number(ratingInput.value),
                comment: elements.reviewComment.value.trim()
            })
        });
        state.pendingReviewTripId = null;
        elements.reviewModal.hidden = true;
        setMessage(result.message);
    } catch (error) {
        elements.reviewError.textContent = error.message;
        elements.reviewError.hidden = false;
        elements.reviewSubmitButton.disabled = false;
    }
}

function buildRouteRequest(includeSchedule = false) {
    const request = {
        originLat: state.origin.lat,
        originLng: state.origin.lng,
        destinationLat: state.destination.lat,
        destinationLng: state.destination.lng,
        originAddress: state.origin.label,
        destinationAddress: state.destination.label,
        vehicleCategory: elements.vehicleCategory.value,
        notes: elements.notes.value.trim()
    };
    if (includeSchedule) {
        request.scheduledAt = elements.scheduledAt.value;
    }
    return request;
}

function renderEstimate(estimate) {
    clearRouteEstimate();
    setTripLockedState(null);

    elements.distance.textContent = `${estimate.distanceKm.toFixed(2)} km`;
    elements.duration.textContent = `${estimate.durationMin} min`;
    elements.price.textContent = formatCurrency(estimate.estimatedPrice);
    elements.taxRate.textContent = formatTaxRate(estimate.taxRate);
    elements.estimatePanel.hidden = false;
    elements.requestButton.disabled = false;
    elements.scheduleButton.disabled = false;

    state.routeLayer = buildRouteLayer(estimate.geometry).addTo(map);
    map.fitBounds(state.routeLayer.getBounds(), { padding: [28, 28] });
}

async function renderActiveTrip(trip) {
    state.activeTripId = trip.tripId;
    state.origin = {
        lat: trip.originLat,
        lng: trip.originLng,
        label: trip.originAddress
    };
    state.destination = {
        lat: trip.destinationLat,
        lng: trip.destinationLng,
        label: trip.destinationAddress
    };

    elements.originInput.value = trip.originAddress;
    elements.destinationInput.value = trip.destinationAddress;
    elements.vehicleCategory.value = trip.vehicleCategory || elements.vehicleCategory.value;
    elements.notes.value = trip.notes || "";
    elements.originStatus.textContent = "Origem da viagem ativa.";
    setMarker("origin", state.origin, "Origem");
    setMarker("destination", state.destination, "Destino");
    renderDriverArrival(trip);
    renderTripPin(trip);

    elements.distance.textContent = `${trip.distanceKm.toFixed(2)} km`;
    elements.duration.textContent = `${trip.durationMin} min`;
    elements.price.textContent = formatCurrency(trip.estimatedPrice);
    elements.taxRate.textContent = formatTaxRate(trip.taxRate);
    elements.estimatePanel.hidden = false;
    elements.requestButton.disabled = true;
    renderTripSummary(trip);
    setTripLockedState(trip.status);

    try {
        const estimate = await fetchJson("/api/routes/estimate", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(buildRouteRequest())
        });
        removeRouteLayer();
        state.routeLayer = buildRouteLayer(estimate.geometry).addTo(map);
        map.fitBounds(state.routeLayer.getBounds(), { padding: [28, 28] });
    } catch {
        map.fitBounds(L.featureGroup([state.originMarker, state.destinationMarker]).getBounds(), { padding: [28, 28] });
    }
}

function buildRouteLayer(geometry) {
    return L.featureGroup([
        { color: "#17202a", weight: 10, opacity: 0.14 },
        { color: "#ffffff", weight: 8, opacity: 0.95 },
        { color: "#2563eb", weight: 5, opacity: 0.95 }
    ].map((style) => L.geoJSON(geometry, {
        pane: "routePane",
        style: { ...style, lineCap: "round", lineJoin: "round" }
    })));
}

function setMarker(type, point, label) {
    const markerKey = type === "origin" ? "originMarker" : "destinationMarker";
    const marker = L.marker([point.lat, point.lng], {
        icon: buildMarkerIcon(type),
        pane: "tripMarkerPane",
        zIndexOffset: 1000
    }).bindPopup(label);

    if (state[markerKey]) {
        state[markerKey].remove();
    }
    state[markerKey] = marker.addTo(map);
}

function buildMarkerIcon(type) {
    return L.divIcon({
        className: "",
        html: `<span class="map-marker map-marker-${type}" aria-hidden="true"></span>`,
        iconSize: [24, 24],
        iconAnchor: [12, 12],
        popupAnchor: [0, -12]
    });
}

function clearRouteEstimate() {
    removeRouteLayer();
    elements.estimatePanel.hidden = true;
    elements.requestButton.disabled = true;
    elements.scheduleButton.disabled = true;
}

function removeRouteLayer() {
    if (state.routeLayer) {
        state.routeLayer.remove();
        state.routeLayer = null;
    }
}

function setBusy(isBusy) {
    const disabled = isBusy || state.tripLocked;
    tripButtons.forEach((button) => {
        button.disabled = disabled || elements.estimatePanel.hidden;
    });
    routeInputs.forEach((input) => {
        input.disabled = disabled;
    });
    elements.locateButton.disabled = disabled;
    elements.cancelTripButton.disabled = isBusy && state.tripLocked;
}

function setTripLockedState(status) {
    state.tripLocked = Boolean(status);
    elements.rideForm.hidden = state.tripLocked;
    elements.estimatePanel.hidden = state.tripLocked || !state.origin || !state.destination;
    elements.waitingPanel.hidden = !state.tripLocked;
    tripButtons.forEach((button) => {
        button.disabled = state.tripLocked || elements.estimatePanel.hidden;
    });
    routeInputs.forEach((input) => {
        input.disabled = state.tripLocked;
    });
    elements.locateButton.hidden = state.tripLocked || Boolean(state.origin);
    elements.cancelTripButton.hidden = !state.tripLocked;
    if (state.tripLocked) {
        startActiveTripPolling();
    } else {
        stopActiveTripPolling();
        renderDriverArrival(null);
        renderTripPin(null);
    }
    ["origin", "destination"].forEach((type) => renderSuggestions(type, []));
    updateWaitingCopy(status);
}

function updateWaitingCopy(status) {
    const title = elements.waitingPanel.querySelector("[data-waiting-title]");
    const detail = elements.waitingPanel.querySelector("[data-waiting-detail]");
    if (status === "ACCEPTED") {
        title.textContent = "Motorista a caminho";
        detail.textContent = "A viagem foi aceite. Diz o PIN ao motorista quando ele chegar.";
    } else if (status === "IN_PROGRESS") {
        title.textContent = "Viagem em curso";
        detail.textContent = "A tua viagem está em progresso.";
    } else {
        title.textContent = "À espera de motorista";
        detail.textContent = "A procurar um motorista disponível para aceitar a viagem.";
    }
}

function renderTripSummary(trip) {
    const notes = elements.notes.value.trim();
    elements.summaryOrigin.textContent = trip.originAddress || state.origin?.label || elements.originInput.value;
    elements.summaryDestination.textContent = trip.destinationAddress
        || state.destination?.label
        || elements.destinationInput.value;
    elements.summaryCategory.textContent = trip.vehicleCategory || elements.vehicleCategory.value;
    elements.summaryDistance.textContent = `${Number(trip.distanceKm || 0).toFixed(2)} km`;
    elements.summaryDuration.textContent = `${trip.durationMin || 0} min`;
    elements.summaryPrice.textContent = formatCurrency(trip.estimatedPrice || 0);
    elements.summaryNotesBlock.hidden = !notes;
    elements.summaryNotes.textContent = notes;
    renderDriverDetails(trip);
}

function renderDriverDetails(trip) {
    const hasDriver = Boolean(trip.driverName);
    elements.driverDetails.hidden = !hasDriver;
    if (!hasDriver) {
        return;
    }

    elements.driverName.textContent = trip.driverName;
    elements.vehicleName.textContent = [trip.vehicleBrand, trip.vehicleModel].filter(Boolean).join(" ") || "Veículo";
    elements.vehicleLicensePlate.textContent = trip.vehicleLicensePlate || "Matrícula por confirmar";
    setAvatar(elements.driverAvatar, trip.driverName, trip.driverPhotoUrl, "M");
}

function setAvatar(element, name, photoUrl, fallback) {
    const hasPhoto = Boolean(photoUrl);
    element.textContent = hasPhoto ? "" : (name?.trim().substring(0, 1).toUpperCase() || fallback);
    element.style.backgroundImage = hasPhoto
        ? `url("${photoUrl.replaceAll('"', "%22")}")`
        : "";
}

async function refreshActiveTrip() {
    if (!state.tripLocked) {
        return;
    }

    try {
        const activeTrip = await fetchJson("/api/trips/active");
        await renderActiveTrip(activeTrip);
    } catch (error) {
        if (error.status === 404) {
            state.activeTripId = null;
            setTripLockedState(null);
            setMessage("");
            await loadPendingReview();
            return;
        }
        setMessage(error.message);
    }
}

function startActiveTripPolling() {
    if (state.activeTripPoll) {
        return;
    }
    state.activeTripPoll = window.setInterval(refreshActiveTrip, 3000);
}

function stopActiveTripPolling() {
    if (state.activeTripPoll) {
        window.clearInterval(state.activeTripPoll);
        state.activeTripPoll = null;
    }
}

function renderTripPin(trip) {
    const pin = trip && trip.startPin ? trip.startPin : "";
    elements.tripPinBlock.hidden = !pin;
    elements.tripPin.textContent = pin || "0000";
}

function renderDriverArrival(trip) {
    const arrivalMin = trip && trip.status === "ACCEPTED" ? trip.driverArrivalMin : null;
    elements.driverArrivalBlock.hidden = arrivalMin == null;
    elements.driverArrival.textContent = arrivalMin == null ? "0 min" : `${arrivalMin} min`;
}

function setMessage(message) {
    elements.message.textContent = message;
    elements.message.hidden = !message;
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, options);
    if (!response.ok) {
        let message = "Não foi possível completar o pedido.";
        try {
            const body = await response.json();
            message = body.message || body.detail || body.error || message;
        } catch {
            if (response.status === 401) {
                message = "Sessão expirada. Volta a iniciar sessão.";
            }
        }
        const error = new Error(message);
        error.status = response.status;
        throw error;
    }
    return response.json();
}

function formatCurrency(value) {
    return new Intl.NumberFormat("pt-PT", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(value) + " €";
}

function formatTaxRate(value) {
    return new Intl.NumberFormat("pt-PT", {
        style: "percent",
        maximumFractionDigits: 2
    }).format(value);
}

function initializeScheduleInput() {
    const now = new Date();
    now.setMinutes(now.getMinutes() + 15);
    const offsetDate = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
    elements.scheduledAt.min = offsetDate.toISOString().slice(0, 16);
}

function formatDateTime(value) {
    if (!value) {
        return "";
    }
    return new Intl.DateTimeFormat("pt-PT", {
        dateStyle: "short",
        timeStyle: "short"
    }).format(new Date(value));
}
