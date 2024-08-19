document.addEventListener('mousemove', function(e) {
  const stars = document.querySelectorAll('.shooting_star');
  const mouseX = e.clientX;
  const mouseY = e.clientY;

  stars.forEach(star => {
    const starRect = star.getBoundingClientRect();
    const starX = starRect.left + starRect.width / 2;
    const starY = starRect.top + starRect.height / 2;

    const deltaX = starX - mouseX;
    const deltaY = starY - mouseY;
    const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    // Adjust this threshold and multiplier as needed for desired effect
    const threshold = 200;
    const maxDistance = 300;
    if (distance < threshold) {
      const pushX = (deltaX / distance) * (maxDistance - distance);
      const pushY = (deltaY / distance) * (maxDistance - distance);
      star.style.transform = `translate(${pushX}px, ${pushY}px)`;
    } else {
      star.style.transform = ''; // Reset the transform if out of range
    }
  });
});
