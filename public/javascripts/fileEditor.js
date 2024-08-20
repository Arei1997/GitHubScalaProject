 document.addEventListener("DOMContentLoaded", function () {
    // Initialize CodeMirror editor in read-only mode initially
    var editor = CodeMirror.fromTextArea(document.getElementById("file-content"), {
        lineNumbers: true,
        mode: "javascript", // Adjust this based on the file content type
        theme: "default",
        readOnly: true // Start in read-only mode
    });

    // Enable editing when "Edit" button is clicked
    document.getElementById("edit-btn").addEventListener("click", function () {
        editor.setOption("readOnly", false); // Enable editing
        document.getElementById("save-btn").style.display = "inline-block"; // Show "Save Changes" button
        this.style.display = "none"; // Hide "Edit" button
    });

    // When the "Save Changes" button is clicked
    document.getElementById("save-btn").addEventListener("click", function () {
        // Set the hidden content field value to the content from the editor
        document.getElementById("hidden-content-form").value = editor.getValue();
        document.getElementById("redirect-form").submit(); // Submit the form
    });
});
