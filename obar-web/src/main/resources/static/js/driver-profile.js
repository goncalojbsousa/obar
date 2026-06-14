const vehicleCreateModal = document.querySelector("[data-vehicle-create-modal]");
const vehicleCreateOpenControls = document.querySelectorAll("[data-vehicle-create-open]");
const vehicleCreateCloseControls = document.querySelectorAll("[data-vehicle-create-close]");
const vehicleEditModal = document.querySelector("[data-vehicle-edit-modal]");
const vehicleEditOpen = document.querySelector("[data-vehicle-edit-open]");
const vehicleEditCloseControls = document.querySelectorAll("[data-vehicle-edit-close]");
const vehicleRemoveModal = document.querySelector("[data-vehicle-remove-modal]");
const vehicleRemoveOpen = document.querySelector("[data-vehicle-remove-open]");
const vehicleRemoveCloseControls = document.querySelectorAll("[data-vehicle-remove-close]");
const vehicleSelector = document.querySelector("[data-vehicle-selector]");
const profileEditModal = document.querySelector("[data-profile-edit-modal]");
const profileEditOpen = document.querySelector("[data-profile-edit-open]");
const profileEditCloseControls = document.querySelectorAll("[data-profile-edit-close]");

if (vehicleCreateModal && vehicleCreateOpenControls.length) {
    const openVehicleCreateModal = () => {
        vehicleCreateModal.hidden = false;
        const firstField = vehicleCreateModal.querySelector("input, select");
        if (firstField) {
            firstField.focus();
        }
    };

    const closeVehicleCreateModal = () => {
        vehicleCreateModal.hidden = true;
        const firstOpenControl = vehicleCreateOpenControls[0];
        if (firstOpenControl) {
            firstOpenControl.focus();
        }
    };

    vehicleCreateOpenControls.forEach((control) => {
        control.addEventListener("click", openVehicleCreateModal);
    });
    vehicleCreateCloseControls.forEach((control) => {
        control.addEventListener("click", closeVehicleCreateModal);
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !vehicleCreateModal.hidden) {
            closeVehicleCreateModal();
        }
    });

    if (vehicleCreateModal.dataset.open === "true") {
        openVehicleCreateModal();
    }
}

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

if (vehicleRemoveModal && vehicleRemoveOpen) {
    const openVehicleRemoveModal = () => {
        vehicleRemoveModal.hidden = false;
        const cancelButton = vehicleRemoveModal.querySelector("[data-vehicle-remove-close]");
        if (cancelButton) {
            cancelButton.focus();
        }
    };

    const closeVehicleRemoveModal = () => {
        vehicleRemoveModal.hidden = true;
        vehicleRemoveOpen.focus();
    };

    vehicleRemoveOpen.addEventListener("click", openVehicleRemoveModal);
    vehicleRemoveCloseControls.forEach((control) => {
        control.addEventListener("click", closeVehicleRemoveModal);
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !vehicleRemoveModal.hidden) {
            closeVehicleRemoveModal();
        }
    });
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
