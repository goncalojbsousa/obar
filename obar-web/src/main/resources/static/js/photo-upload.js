document.querySelectorAll("[data-photo-upload]").forEach((input) => {
    input.addEventListener("change", () => {
        if (!input.files || input.files.length === 0) {
            return;
        }

        const form = input.closest("form");
        if (form) {
            form.requestSubmit();
        }
    });
});
