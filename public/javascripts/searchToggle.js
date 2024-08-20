document.addEventListener('DOMContentLoaded', function() {
  const searchIcon = document.getElementById('search-icon');
  const searchFormOverlay = document.getElementById('search-form-overlay');
  const searchForm = document.getElementById('search-form'); // Ensure this is correctly referenced

  searchIcon.addEventListener('click', function() {
    if (searchFormOverlay.style.display === 'flex') {
      searchFormOverlay.style.display = 'none';
    } else {
      searchFormOverlay.style.display = 'flex';
      searchForm.style.display = 'flex'; // Ensure the form is also displayed
      document.getElementById('search-input').focus(); // Automatically focus the input
    }
  });

  searchFormOverlay.addEventListener('click', function(event) {
    // Hide the form when clicking outside of the actual form area
    if (event.target === searchFormOverlay) {
      searchFormOverlay.style.display = 'none';
      searchForm.style.display = 'none'; // Hide the form as well
    }
  });
});
