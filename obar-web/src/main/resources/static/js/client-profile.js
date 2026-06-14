const driverUpgradeModal = document.querySelector("[data-driver-upgrade-modal]");
const driverUpgradeOpen = document.querySelector("[data-driver-upgrade-open]");
const driverUpgradeCloseControls = document.querySelectorAll("[data-driver-upgrade-close]");

if (driverUpgradeModal && driverUpgradeOpen) {
    const openDriverUpgradeModal = () => {
        driverUpgradeModal.hidden = false;
        const firstField = driverUpgradeModal.querySelector("input, select");
        if (firstField) {
            firstField.focus();
        }
    };

    const closeDriverUpgradeModal = () => {
        driverUpgradeModal.hidden = true;
        driverUpgradeOpen.focus();
    };

    driverUpgradeOpen.addEventListener("click", openDriverUpgradeModal);
    driverUpgradeCloseControls.forEach((control) => {
        control.addEventListener("click", closeDriverUpgradeModal);
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !driverUpgradeModal.hidden) {
            closeDriverUpgradeModal();
        }
    });

    if (driverUpgradeModal.dataset.open === "true") {
        openDriverUpgradeModal();
    }
}
