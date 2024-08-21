document.addEventListener("DOMContentLoaded", function () {
    var toggleSwitch = document.getElementById("toggle-dark-mode");

    // Load saved theme from local storage
    if (localStorage.getItem("theme") === "dark") {
        document.body.classList.add("dark-mode");
        toggleSwitch.checked = true; // Set the switch to checked if dark mode is enabled
    }

    // Toggle dark mode
    toggleSwitch.addEventListener("change", function () {
        document.body.classList.toggle("dark-mode");

        // Save the preference in local storage
        if (document.body.classList.contains("dark-mode")) {
            localStorage.setItem("theme", "dark");
        } else {
            localStorage.setItem("theme", "light");
        }
    });
});
