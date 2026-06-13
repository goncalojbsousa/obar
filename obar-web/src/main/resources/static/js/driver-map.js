const defaultCenter = [41.1579, -8.6291];
const responseTimeoutSeconds = 30;

const state = {
    assignment: null,
    routeLayer: null,
    markersLayer: L.layerGroup(),
    assignmentMarkers: L.layerGroup(),
    assignmentPoll: null,
    snapshotPoll: null,
    countdownTimer: null,
    currentLocation: null,
    online: false
};

const map = L.map("driver-map", { zoomControl: true }).setView(defaultCenter, 12);
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
state.markersLayer.addTo(map);
state.assignmentMarkers.addTo(map);

const elements = {
    status: document.querySelector("[data-driver-status]"),
    empty: document.querySelector("[data-driver-empty]"),
    trip: document.querySelector("[data-driver-trip]"),
    countdown: document.querySelector("[data-countdown]"),
    originAddress: document.querySelector("[data-origin-address]"),
    destinationAddress: document.querySelector("[data-destination-address]"),
    distance: document.querySelector("[data-distance]"),
    duration: document.querySelector("[data-duration]"),
    price: document.querySelector("[data-price]"),
    category: document.querySelector("[data-category]"),
    clientName: document.querySelector("[data-client-name]"),
    clientRating: document.querySelector("[data-client-rating]"),
    countdownBlock: document.querySelector("[data-countdown-block]"),
    responseActions: document.querySelector("[data-response-actions]"),
    acceptButton: document.querySelector("[data-accept-trip]"),
    rejectButton: document.querySelector("[data-reject-trip]"),
    startPanel: document.querySelector("[data-start-panel]"),
    startPin: document.querySelector("[data-start-pin]"),
    startButton: document.querySelector("[data-start-trip]"),
    completePanel: document.querySelector("[data-complete-panel]"),
    clientRatingInput: document.querySelector("[data-client-rating-input]"),
    completeButton: document.querySelector("[data-complete-trip]"),
    cancelTripButton: document.querySelector("[data-cancel-trip]"),
    message: document.querySelector("[data-driver-message]"),
    alertModal: document.querySelector("[data-alert-modal]"),
    alertTitle: document.querySelector("[data-alert-title]"),
    alertMessage: document.querySelector("[data-alert-message]"),
    alertCloseButtons: document.querySelectorAll("[data-alert-close]"),
    cancelModal: document.querySelector("[data-cancel-modal]"),
    cancelForm: document.querySelector("[data-cancel-form]"),
    cancelReason: document.querySelector("[data-cancel-reason]"),
    cancelError: document.querySelector("[data-cancel-error]"),
    cancelSubmitButton: document.querySelector("[data-cancel-submit]"),
    cancelDismissButtons: document.querySelectorAll("[data-cancel-dismiss]")
};

elements.acceptButton.addEventListener("click", acceptAssignment);
elements.rejectButton.addEventListener("click", rejectAssignment);
elements.startButton.addEventListener("click", startTrip);
elements.completeButton.addEventListener("click", completeTrip);
elements.cancelTripButton.addEventListener("click", openCancelModal);
elements.cancelForm.addEventListener("submit", submitCancelTrip);
elements.alertCloseButtons.forEach((button) => button.addEventListener("click", closeAlertModal));
elements.cancelDismissButtons.forEach((button) => button.addEventListener("click", closeCancelModal));
elements.startPin.addEventListener("input", () => {
    elements.startPin.value = elements.startPin.value.replace(/\D/g, "").slice(0, 4);
});
document.addEventListener("keydown", handleModalKeydown);
document.addEventListener("driver-online-changed", (event) => {
    renderOnlineStatus(event.detail.online);
    if (event.detail.online) {
        pollAssignment();
    }
});

initializeDriverMap();

async function initializeDriverMap() {
    await loadOnlineStatus();
    if (state.online) {
        await updateCurrentLocation({ silent: true });
    }
    loadSnapshot();
    pollAssignment();
    state.assignmentPoll = window.setInterval(pollAssignment, 2500);
    state.snapshotPoll = window.setInterval(() => {
        if (!state.assignment) {
            loadSnapshot();
        }
    }, 10000);
}

function updateCurrentLocation(options = {}) {
    const silent = options.silent === true;
    if (!navigator.geolocation) {
        return Promise.resolve(null);
    }

    return new Promise((resolve) => navigator.geolocation.getCurrentPosition(
        async (position) => {
            const point = {
                lat: position.coords.latitude,
                lng: position.coords.longitude
            };
            state.currentLocation = point;
            map.setView([point.lat, point.lng], 13);
            try {
                await fetchJson("/api/driver/location", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(point)
                });
                resolve(point);
            } catch (error) {
                if (!silent) {
                    setMessage(error.message);
                }
                resolve(point);
            }
        },
        () => resolve(state.currentLocation),
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    ));
}

async function pollAssignment() {
    if (state.assignment || !state.online) {
        return;
    }

    try {
        const assignment = await fetchJson("/api/driver/assignment");
        renderAssignment(assignment);
    } catch (error) {
        if (error.status !== 404) {
            setMessage(error.message);
        }
    }
}

async function loadOnlineStatus() {
    try {
        const result = await fetchJson("/api/driver/online");
        renderOnlineStatus(result.online);
    } catch (error) {
        setMessage(error.message);
    }
}

function renderOnlineStatus(online) {
    state.online = online;
    if (!state.assignment) {
        elements.status.textContent = online
            ? "A procurar viagens para ti."
            : "Fica online quando quiseres trabalhar.";
    }
}

async function loadSnapshot() {
    try {
        const snapshot = await fetchJson("/api/driver/map-snapshot");
        renderSnapshot(snapshot);
    } catch (error) {
        setMessage(error.message);
    }
}

function renderSnapshot(snapshot) {
    if (state.assignment) {
        return;
    }

    state.markersLayer.clearLayers();
    const markers = [];
    [...snapshot.drivers, ...snapshot.clients].forEach((point) => {
        const marker = L.marker([point.lat, point.lng], {
            icon: buildMarkerIcon(point.type.toLowerCase()),
            pane: "tripMarkerPane"
        }).bindPopup(point.name);
        marker.addTo(state.markersLayer);
        markers.push(marker);
    });

    if (markers.length > 0) {
        map.fitBounds(L.featureGroup(markers).getBounds(), { padding: [36, 36], maxZoom: 13 });
    }
}

function renderAssignment(assignment) {
    state.assignment = assignment;
    state.markersLayer.clearLayers();
    state.assignmentMarkers.clearLayers();
    clearRouteLayer();
    setMessage("");

    elements.status.textContent = statusCopy(assignment);
    elements.empty.hidden = true;
    elements.trip.hidden = false;
    elements.originAddress.textContent = assignment.originAddress;
    elements.destinationAddress.textContent = assignment.destinationAddress;
    elements.distance.textContent = `${assignment.distanceKm.toFixed(2)} km`;
    elements.duration.textContent = `${assignment.durationMin} min`;
    elements.price.textContent = formatCurrency(assignment.estimatedPrice);
    elements.category.textContent = assignment.vehicleCategory;
    elements.clientName.textContent = assignment.clientName;
    elements.clientRating.textContent = assignment.clientRating > 0
        ? `★ ${assignment.clientRating.toFixed(1)}`
        : "Sem avaliações";
    const isPending = assignment.status === "PENDING";
    const isAccepted = assignment.status === "ACCEPTED";
    const isInProgress = assignment.status === "IN_PROGRESS";
    const canCancel = assignment.status === "ACCEPTED" || assignment.status === "IN_PROGRESS";
    elements.countdownBlock.hidden = !isPending;
    elements.responseActions.hidden = !isPending;
    elements.startPanel.hidden = !isAccepted;
    elements.completePanel.hidden = !isInProgress;
    elements.cancelTripButton.hidden = !canCancel;
    elements.acceptButton.disabled = !isPending;
    elements.rejectButton.disabled = !isPending;
    elements.startButton.disabled = !isAccepted;
    elements.completeButton.disabled = !isInProgress;
    elements.cancelTripButton.disabled = !canCancel;

    state.routeLayer = buildRouteLayer(assignment.geometry).addTo(map);
    const originMarker = L.marker([assignment.originLat, assignment.originLng], {
        icon: buildMarkerIcon(assignment.routeMode === "PICKUP" ? "driver" : "client"),
        pane: "tripMarkerPane"
    }).bindPopup(assignment.routeMode === "PICKUP" ? "Motorista" : "Origem");
    const destinationMarker = L.marker([assignment.destinationLat, assignment.destinationLng], {
        icon: buildMarkerIcon(assignment.routeMode === "PICKUP" ? "client" : "destination"),
        pane: "tripMarkerPane"
    }).bindPopup(assignment.routeMode === "PICKUP" ? "Cliente" : "Destino");
    originMarker.addTo(state.assignmentMarkers);
    destinationMarker.addTo(state.assignmentMarkers);
    map.fitBounds(L.featureGroup([state.routeLayer, originMarker, destinationMarker]).getBounds(), { padding: [36, 36] });

    if (isPending) {
        startCountdown(assignment.secondsLeft);
    } else {
        stopCountdown();
    }
}

async function acceptAssignment() {
    setActionBusy(true);
    try {
        const point = await updateCurrentLocation({ silent: true });
        const options = { method: "POST" };
        if (point) {
            options.headers = { "Content-Type": "application/json" };
            options.body = JSON.stringify(point);
        }
        const assignment = await fetchJson("/api/driver/assignment/accept", options);
        renderAssignment(assignment);
        setMessage("Viagem aceite. Segue até ao cliente e introduz o PIN para começar.");
    } catch (error) {
        setMessage(error.message);
        setActionBusy(false);
    }
}

async function startTrip() {
    const pin = elements.startPin.value.trim();
    if (pin.length !== 4) {
        setMessage("Introduz o PIN de 4 dígitos indicado pelo cliente.");
        return;
    }

    elements.startButton.disabled = true;
    setMessage("");
    try {
        const assignment = await fetchJson("/api/driver/assignment/start", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ pin })
        });
        elements.startPin.value = "";
        renderAssignment(assignment);
        setMessage("Viagem iniciada. Segue a rota até ao destino.");
    } catch (error) {
        showAlertModal(
            error.message.toLowerCase().includes("pin") ? "PIN inválido" : "Não foi possível começar",
            error.message
        );
        elements.startPin.select();
        elements.startButton.disabled = false;
    }
}

async function completeTrip() {
    const rating = Number(elements.clientRatingInput.value);
    if (rating < 1 || rating > 5) {
        setMessage("Escolhe uma nota para o cliente.");
        return;
    }

    elements.completeButton.disabled = true;
    try {
        const result = await fetchJson("/api/driver/assignment/complete", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ rating })
        });
        resetAssignmentView();
        setMessage(result.message);
        loadSnapshot();
    } catch (error) {
        setMessage(error.message);
        elements.completeButton.disabled = false;
    }
}

async function rejectAssignment() {
    setActionBusy(true);
    try {
        const result = await fetchJson("/api/driver/assignment/reject", { method: "POST" });
        resetAssignmentView();
        setMessage(result.message);
        loadSnapshot();
    } catch (error) {
        setMessage(error.message);
        setActionBusy(false);
    }
}

function openCancelModal() {
    if (!state.assignment) {
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
    if (!state.assignment) {
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
    elements.cancelTripButton.disabled = true;
    elements.cancelError.hidden = true;
    setMessage("A cancelar viagem...");
    try {
        const result = await fetchJson("/api/driver/assignment/cancel", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ reason })
        });
        elements.cancelModal.hidden = true;
        resetAssignmentView();
        setMessage(result.message);
        loadSnapshot();
    } catch (error) {
        elements.cancelError.textContent = error.message;
        elements.cancelError.hidden = false;
        elements.cancelSubmitButton.disabled = false;
        elements.cancelTripButton.disabled = false;
    }
}

function startCountdown(secondsLeft) {
    stopCountdown();
    let remaining = Math.min(responseTimeoutSeconds, Math.max(0, secondsLeft));
    renderCountdown(remaining);
    state.countdownTimer = window.setInterval(() => {
        remaining -= 1;
        renderCountdown(remaining);
        if (remaining <= 0) {
            stopCountdown();
            resetAssignmentView();
            setMessage("O tempo para responder terminou. A viagem pode ser atribuída a outro motorista.");
            window.setTimeout(pollAssignment, 1200);
        }
    }, 1000);
}

function renderCountdown(seconds) {
    const safeSeconds = Math.max(0, seconds);
    elements.countdown.textContent = `00:${String(safeSeconds).padStart(2, "0")}`;
}

function stopCountdown() {
    if (state.countdownTimer) {
        window.clearInterval(state.countdownTimer);
        state.countdownTimer = null;
    }
}

function resetAssignmentView() {
    state.assignment = null;
    stopCountdown();
    clearRouteLayer();
    state.assignmentMarkers.clearLayers();
    elements.status.textContent = "A procurar viagens para ti.";
    elements.empty.hidden = false;
    elements.trip.hidden = true;
    elements.countdownBlock.hidden = false;
    elements.responseActions.hidden = false;
    elements.startPanel.hidden = true;
    elements.completePanel.hidden = true;
    elements.cancelTripButton.hidden = true;
    elements.startPin.value = "";
    elements.clientRatingInput.value = "";
    setActionBusy(false);
}

function statusCopy(assignment) {
    if (assignment.status === "ACCEPTED") {
        return "Vai até à localização do cliente.";
    }
    if (assignment.status === "IN_PROGRESS") {
        return "Viagem em curso.";
    }
    return "Tens uma viagem para responder.";
}

function buildRouteLayer(geometry) {
    return L.featureGroup([
        L.geoJSON(geometry, {
            pane: "routePane",
            style: { color: "#17202a", weight: 10, opacity: 0.18, lineCap: "round", lineJoin: "round" }
        }),
        L.geoJSON(geometry, {
            pane: "routePane",
            style: { color: "#ffffff", weight: 8, opacity: 0.95, lineCap: "round", lineJoin: "round" }
        }),
        L.geoJSON(geometry, {
            pane: "routePane",
            style: { color: "#2563eb", weight: 5, opacity: 0.95, lineCap: "round", lineJoin: "round" }
        })
    ]);
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

function clearRouteLayer() {
    if (state.routeLayer) {
        state.routeLayer.remove();
        state.routeLayer = null;
    }
}

function setActionBusy(isBusy) {
    elements.acceptButton.disabled = isBusy;
    elements.rejectButton.disabled = isBusy;
}

function setMessage(message) {
    elements.message.textContent = message;
    elements.message.hidden = !message;
}

function showAlertModal(title, message) {
    elements.alertTitle.textContent = title;
    elements.alertMessage.textContent = message || "Não foi possível completar o pedido.";
    elements.alertModal.hidden = false;
    const closeButton = elements.alertModal.querySelector("[data-alert-close]:last-child");
    window.setTimeout(() => closeButton.focus(), 0);
}

function closeAlertModal() {
    elements.alertModal.hidden = true;
}

function handleModalKeydown(event) {
    if (event.key !== "Escape") {
        return;
    }

    if (!elements.alertModal.hidden) {
        closeAlertModal();
        return;
    }

    if (!elements.cancelModal.hidden) {
        closeCancelModal();
    }
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
        style: "currency",
        currency: "EUR"
    }).format(value);
}
