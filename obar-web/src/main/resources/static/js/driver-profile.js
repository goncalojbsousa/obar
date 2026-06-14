const vehicleEditModal = document.querySelector("[data-vehicle-edit-modal]");
const vehicleEditOpen = document.querySelector("[data-vehicle-edit-open]");
const vehicleEditCloseControls = document.querySelectorAll("[data-vehicle-edit-close]");
const vehicleSelector = document.querySelector("[data-vehicle-selector]");
const profileEditModal = document.querySelector("[data-profile-edit-modal]");
const profileEditOpen = document.querySelector("[data-profile-edit-open]");
const profileEditCloseControls = document.querySelectorAll("[data-profile-edit-close]");

if (vehicleEditModal && vehicleEditOpen) {
    const openVehicleEditModal = () => {
        vehicleEditModal.hidden = false;
        const firstField = vehicleEditModal.querySelector("input, select");
        if (firstField) {
            firstField.focus();
        }
    };

    const closeVehicleEditModal = () => {
        vehicleEditModal.hidden = true;
        vehicleEditOpen.focus();
    };

    vehicleEditOpen.addEventListener("click", openVehicleEditModal);
    vehicleEditCloseControls.forEach((control) => {
        control.addEventListener("click", closeVehicleEditModal);
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !vehicleEditModal.hidden) {
            closeVehicleEditModal();
        }
    });

    if (vehicleEditModal.dataset.open === "true") {
        openVehicleEditModal();
    }
}

if (vehicleSelector) {
    vehicleSelector.addEventListener("change", () => {
        const form = vehicleSelector.closest("[data-vehicle-selector-form]");
        if (form) {
            form.requestSubmit();
        }
    });
}

if (profileEditModal && profileEditOpen) {
    const openProfileEditModal = () => {
        profileEditModal.hidden = false;
        const firstField = profileEditModal.querySelector("input, select");
        if (firstField) {
            firstField.focus();
        }
    };

    const closeProfileEditModal = () => {
        profileEditModal.hidden = true;
        profileEditOpen.focus();
    };

    profileEditOpen.addEventListener("click", openProfileEditModal);
    profileEditCloseControls.forEach((control) => {
        control.addEventListener("click", closeProfileEditModal);
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !profileEditModal.hidden) {
            closeProfileEditModal();
        }
    });

    if (profileEditModal.dataset.open === "true") {
        openProfileEditModal();
    }
}
