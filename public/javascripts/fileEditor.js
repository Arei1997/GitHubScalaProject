document.addEventListener("DOMContentLoaded", function () {
    var editor = CodeMirror.fromTextArea(document.getElementById("file-content"), {
        lineNumbers: true,
        mode: "javascript", // Adjust this based on the file content type
        theme: "default",
    });

    document.getElementById("save-btn").addEventListener("click", function () {
        // Set the hidden content field value to the content from the editor
        document.getElementById("hidden-content").value = editor.getValue();
        document.getElementById("redirect-form").submit(); // Submit the form
    });
});
