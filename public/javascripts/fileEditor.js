document.addEventListener("DOMContentLoaded", function () {
    // Determine if we are creating a new file or editing an existing one
    var isCreatingFile = document.getElementById("file-content").value === ""; // Assumes an empty content indicates a new file

    // Initialize CodeMirror editor
    var editor = CodeMirror.fromTextArea(document.getElementById("file-content"), {
        lineNumbers: true,
        mode: "javascript", // Adjust this based on the file type
        theme: "default",   // Set the theme to default to ensure a white background
        readOnly: !isCreatingFile // Editable if creating, read-only if editing
    });

    // Enable editing when "Edit" button is clicked (for editing an existing file)
    var editBtn = document.getElementById("edit-btn");
    if (editBtn) {
        editBtn.addEventListener("click", function () {
            editor.setOption("readOnly", false); // Enable editing
            document.getElementById("save-btn").style.display = "inline-block"; // Show "Save Changes" button
            this.style.display = "none"; // Hide "Edit" button
        });
    }

    // Handle form submission to update content from editor
    document.querySelector("form").addEventListener("submit", function () {
        document.getElementById("file-content").value = editor.getValue();
    });

    // When the "Save Changes" button is clicked, update the content and submit (for editing)
    var saveBtn = document.getElementById("save-btn");
    if (saveBtn) {
        saveBtn.addEventListener("click", function () {
            document.getElementById("hidden-content-form").value = editor.getValue();
            document.getElementById("redirect-form").submit(); // Submit the form
        });
    }
});
