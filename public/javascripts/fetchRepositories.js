document.addEventListener("DOMContentLoaded", function() {
    const container = document.getElementById('top-repositories');
    const githubApiUrl = 'https://api.github.com/search/repositories?q=language:scala&sort=stars&order=desc&per_page=5';

    fetch(githubApiUrl)
        .then(response => response.json())
        .then(data => {
            if (data.items && data.items.length > 0) {
                const repoList = document.createElement('ul');
                repoList.className = 'repo-list';
                data.items.forEach(repo => {
                    const listItem = document.createElement('li');
                    listItem.className = 'repo-item';
                    listItem.innerHTML = `
                        <a href="${repo.html_url}" target="_blank" class="repo-link">${repo.name}</a>
                        <p class="repo-description">${repo.description || "No description available"}</p>
                    `;
                    repoList.appendChild(listItem);
                });
                container.innerHTML = ''; // Clear loading text
                container.appendChild(repoList);
            } else {
                container.innerHTML = '<p>No repositories found.</p>';
            }
        })
        .catch(error => {
            container.innerHTML = '<p>Error loading repositories.</p>';
            console.error('Error fetching repositories:', error);
        });
});
