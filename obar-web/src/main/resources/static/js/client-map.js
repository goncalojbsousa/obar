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
    tripLocked: false
};

const map = L.map("ride-map", { zoomControl: true }).setView(defaultCenter, 13);
map.createPane("routePane");
map.getPane("routePane").style.zIndex = 450;
map.getPane("routePane").style.pointerEvents = "none";
map.createPane("tripMarkerPane");
map.getPane("tripMarkerPane").style.zIndex = 720;

L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors"
}).addTo(map);

const elements = {
    originStatus: document.querySelector("[data-origin-status]"),
    originInput: document.querySelector("[data-origin-input]"),
    originSuggestions: document.querySelector("[data-origin-suggestions]"),
    locateButton: document.querySelector("[data-locate-button]"),
    destinationInput: document.querySelector("[data-destination-input]"),
    destinationSuggestions: document.querySelector("[data-location-suggestions]"),
    vehicleCategory: document.querySelector("[data-vehicle-category]"),
    scheduledAt: document.querySelector("[data-scheduled-at]"),
    requestButton: document.querySelector("[data-request-button]"),
    scheduleButton: document.querySelector("[data-schedule-button]"),
    estimatePanel: document.querySelector("[data-estimate-panel]"),
    distance: document.querySelector("[data-distance]"),
    duration: document.querySelector("[data-duration]"),
    price: document.querySelector("[data-price]"),
    message: document.querySelector("[data-ride-message]"),
    waitingPanel: document.querySelector("[data-waiting-panel]"),
    cancelTripButton: document.querySelector("[data-cancel-trip-button]")
};

elements.locateButton.addEventListener("click", locateUser);
elements.originInput.addEventListener("input", (event) => handleLocationInput("origin", event));
elements.destinationInput.addEventListener("input", (event) => handleLocationInput("destination", event));
elements.vehicleCategory.addEventListener("change", () => {
    if (state.origin && state.destination) {
        estimateRoute();
    }
});
elements.requestButton.addEventListener("click", requestTrip);
elements.scheduleButton.addEventListener("click", scheduleTrip);
elements.cancelTripButton.addEventListener("click", cancelTrip);

initializeScheduleInput();
initializeRide();

async function initializeRide() {
    try {
        const activeTrip = await fetchJson("/api/trips/active");
        await renderActiveTrip(activeTrip);
    } catch (error) {
        if (error.status === 404) {
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
        setTripLockedState(trip.status);
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
        return;
    }

    setBusy(true);
    setMessage("");
    try {
        const trip = await fetchJson("/api/trips/schedule", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(buildRouteRequest({ scheduled: true }))
        });
        setMessage(`Viagem agendada para ${formatDateTime(trip.scheduledAt)}. Podes continuar a pedir viagens imediatas.`);
    } catch (error) {
        setMessage(error.message);
    } finally {
        setBusy(false);
    }
}

async function cancelTrip() {
    if (!state.activeTripId) {
        return;
    }

    setBusy(true);
    elements.cancelTripButton.disabled = true;
    setMessage("A cancelar pedido...");
    try {
        await fetchJson(`/api/trips/${state.activeTripId}/cancel`, {
            method: "POST"
        });
        state.activeTripId = null;
        setTripLockedState(null);
        setMessage("Pedido cancelado. Podes escolher outro destino ou pedir novamente.");
    } catch (error) {
        setMessage(error.message);
        elements.cancelTripButton.disabled = false;
    } finally {
        setBusy(false);
    }
}

function buildRouteRequest(options = {}) {
    const request = {
        originLat: state.origin.lat,
        originLng: state.origin.lng,
        destinationLat: state.destination.lat,
        destinationLng: state.destination.lng,
        originAddress: state.origin.label,
        destinationAddress: state.destination.label,
        vehicleCategory: elements.vehicleCategory.value
    };
    if (options.scheduled) {
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
    elements.originStatus.textContent = "Origem da viagem ativa.";
    setMarker("origin", state.origin, "Origem");
    setMarker("destination", state.destination, "Destino");
    setTripLockedState(trip.status);

    elements.distance.textContent = `${trip.distanceKm.toFixed(2)} km`;
    elements.duration.textContent = `${trip.durationMin} min`;
    elements.price.textContent = formatCurrency(trip.estimatedPrice);
    elements.estimatePanel.hidden = false;
    elements.requestButton.disabled = true;

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
    const routeOutline = L.geoJSON(geometry, {
        pane: "routePane",
        style: {
            color: "#ffffff",
            weight: 8,
            opacity: 0.95,
            lineCap: "round",
            lineJoin: "round"
        }
    });
    const routeShadow = L.geoJSON(geometry, {
        pane: "routePane",
        style: {
            color: "#17202a",
            weight: 10,
            opacity: 0.14,
            lineCap: "round",
            lineJoin: "round"
        }
    });
    const routeLine = L.geoJSON(geometry, {
        pane: "routePane",
        style: {
            color: "#2563eb",
            weight: 5,
            opacity: 0.95,
            lineCap: "round",
            lineJoin: "round"
        }
    });

    return L.featureGroup([routeShadow, routeOutline, routeLine]);
}

function setMarker(type, point, label) {
    const marker = L.marker([point.lat, point.lng], {
        icon: buildMarkerIcon(type),
        pane: "tripMarkerPane",
        zIndexOffset: 1000
    }).bindPopup(label);
    if (type === "origin") {
        if (state.originMarker) {
            state.originMarker.remove();
        }
        state.originMarker = marker.addTo(map);
    } else {
        if (state.destinationMarker) {
            state.destinationMarker.remove();
        }
        state.destinationMarker = marker.addTo(map);
    }
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
    elements.requestButton.disabled = isBusy || state.tripLocked || elements.estimatePanel.hidden;
    elements.scheduleButton.disabled = isBusy || state.tripLocked || elements.estimatePanel.hidden;
    elements.destinationInput.disabled = isBusy || state.tripLocked;
    elements.originInput.disabled = isBusy || state.tripLocked;
    elements.vehicleCategory.disabled = isBusy || state.tripLocked;
    elements.scheduledAt.disabled = isBusy || state.tripLocked;
    elements.locateButton.disabled = isBusy || state.tripLocked;
    elements.cancelTripButton.disabled = isBusy && state.tripLocked;
}

function setTripLockedState(status) {
    const isLocked = Boolean(status);
    state.tripLocked = isLocked;
    elements.waitingPanel.hidden = !isLocked;
    elements.requestButton.disabled = isLocked || elements.estimatePanel.hidden;
    elements.scheduleButton.disabled = isLocked || elements.estimatePanel.hidden;
    elements.destinationInput.disabled = isLocked;
    elements.originInput.disabled = isLocked;
    elements.vehicleCategory.disabled = isLocked;
    elements.scheduledAt.disabled = isLocked;
    elements.locateButton.hidden = isLocked || Boolean(state.origin);
    elements.cancelTripButton.hidden = status === "IN_PROGRESS";
    renderSuggestions("origin", []);
    renderSuggestions("destination", []);
    updateWaitingCopy(status);
}

function updateWaitingCopy(status) {
    const title = elements.waitingPanel.querySelector("[data-waiting-title]");
    const detail = elements.waitingPanel.querySelector("p");
    if (status === "ACCEPTED") {
        title.textContent = "Motorista a caminho";
        detail.textContent = "A viagem foi aceite. Aguarda a chegada do motorista.";
    } else if (status === "IN_PROGRESS") {
        title.textContent = "Viagem em curso";
        detail.textContent = "A tua viagem está em progresso.";
    } else {
        title.textContent = "À espera de motorista";
        detail.textContent = "A procurar um motorista disponível para aceitar a viagem.";
    }
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
        style: "currency",
        currency: "EUR"
    }).format(value);
}

function initializeScheduleInput() {
    const now = new Date();
    now.setMinutes(now.getMinutes() + 15);
    elements.scheduledAt.min = toLocalDateTimeValue(now);
}

function toLocalDateTimeValue(date) {
    const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return offsetDate.toISOString().slice(0, 16);
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
