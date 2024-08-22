document.addEventListener("DOMContentLoaded", function() {
    const toggleSwitch = document.getElementById("toggle-dark-mode");
    const body = document.body;

    // Load the saved mode (if any) from localStorage
    const currentMode = localStorage.getItem("mode");
    if (currentMode) {
        body.classList.add(currentMode);
        toggleSwitch.checked = currentMode === "dark-mode";
    }

    toggleSwitch.addEventListener("change", function() {
        if (toggleSwitch.checked) {
            body.classList.add("dark-mode");
            localStorage.setItem("mode", "dark-mode");
        } else {
            body.classList.remove("dark-mode");
            localStorage.setItem("mode", "light-mode");
        }
    });
});
