(function () {
const onlineInput = document.querySelector("[data-driver-online]");
const clientHomeLink = document.querySelector("[data-client-home]");

if (!onlineInput) {
    return;
}

onlineInput.addEventListener("change", updateOnlineStatus);
loadOnlineStatus();
clientHomeLink?.addEventListener("click", (event) => onlineInput.checked && event.preventDefault());

async function loadOnlineStatus() {
    try {
        const result = await fetchOnlineJson("/api/driver/online");
        setOnlineStatus(result.online);
    } catch (error) {
        window.alert(error.message);
    }
}

async function updateOnlineStatus() {
    const online = onlineInput.checked;
    onlineInput.disabled = true;

    try {
        const location = online ? await getCurrentLocation() : null;
        if (online && !location) {
            throw new Error("Ativa a localização para ficares online.");
        }

        const result = await fetchOnlineJson("/api/driver/online", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                online,
                lat: location?.lat ?? null,
                lng: location?.lng ?? null
            })
        });
        setOnlineStatus(result.online);
    } catch (error) {
        onlineInput.checked = !online;
        window.alert(error.message);
    } finally {
        onlineInput.disabled = false;
    }
}

function setOnlineStatus(online) {
    onlineInput.checked = online;
    if (clientHomeLink) {
        online ? clientHomeLink.setAttribute("aria-disabled", "true") : clientHomeLink.removeAttribute("aria-disabled");
    }
    document.dispatchEvent(new CustomEvent("driver-online-changed", {
        detail: { online }
    }));

    if (online && window.location.pathname === "/app") {
        window.location.href = "/driver";
    }
}

function getCurrentLocation() {
    if (!navigator.geolocation) {
        return Promise.resolve(null);
    }

    return new Promise((resolve) => navigator.geolocation.getCurrentPosition(
        (position) => resolve({
            lat: position.coords.latitude,
            lng: position.coords.longitude
        }),
        () => resolve(null),
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    ));
}

async function fetchOnlineJson(url, options = {}) {
    const response = await fetch(url, options);
    if (!response.ok) {
        let message = "Não foi possível alterar o estado online.";
        try {
            const body = await response.json();
            message = body.message || body.detail || body.error || message;
        } catch {
        }
        throw new Error(message);
    }
    return response.json();
}
})();
