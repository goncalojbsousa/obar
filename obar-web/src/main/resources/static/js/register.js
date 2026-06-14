(() => {
    const form = document.querySelector(".register-form");
    if (!form) {
        return;
    }

    const accountTypeInputs = Array.from(form.querySelectorAll("[data-account-type]"));
    const panels = Array.from(form.querySelectorAll("[data-register-panel]"));
    const title = document.getElementById("register-title");
    const subtitle = document.querySelector(".login-heading p");
    const sharedFieldNames = ["name", "email", "phone"];

    const selectedType = () => {
        const selectedInput = accountTypeInputs.find((input) => input.checked);
        return selectedInput ? selectedInput.value : "CLIENT";
    };

    const activePanel = () => panels.find((panel) => !panel.hidden);

    const panelFor = (type) => panels.find((panel) => panel.dataset.registerPanel === type);

    const copySharedValues = (fromPanel, toPanel) => {
        if (!fromPanel || !toPanel || fromPanel === toPanel) {
            return;
        }

        sharedFieldNames.forEach((name) => {
            const source = fromPanel.querySelector(`[name="${name}"]`);
            const target = toPanel.querySelector(`[name="${name}"]`);
            if (source && target) {
                target.value = source.value;
            }
        });
    };

    const setPanelState = (panel, isActive) => {
        panel.hidden = !isActive;
        panel.disabled = !isActive;
        panel.querySelectorAll("input, select, textarea").forEach((field) => {
            field.disabled = !isActive;
        });
    };

    const syncAccountType = () => {
        const type = selectedType();
        const nextPanel = panelFor(type);
        copySharedValues(activePanel(), nextPanel);

        panels.forEach((panel) => {
            setPanelState(panel, panel === nextPanel);
        });

        accountTypeInputs.forEach((input) => {
            const option = input.closest(".register-type-option");
            if (option) {
                option.classList.toggle("is-active", input.checked);
            }
        });

        const isDriver = type === "DRIVER";
        if (title) {
            title.textContent = isDriver ? "Criar conta motorista" : "Criar conta cliente";
        }
        if (subtitle) {
            subtitle.textContent = isDriver
                ? "A tua conta fica pendente ate um admin aprovar no desktop."
                : "Cria uma conta para pedir viagens pela web.";
        }
    };

    accountTypeInputs.forEach((input) => input.addEventListener("change", syncAccountType));
    syncAccountType();
})();
