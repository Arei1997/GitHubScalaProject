# GitHub Scala API Project

This project is a RESTful API built with the Play Framework in Scala. It interacts with the GitHub API to retrieve and display information about repositories, including the top 5 most popular Scala repositories. The API also includes basic CRUD operations for managing users stored in MongoDB, allowing you to fetch, create, update, and delete users.

## Running the project

To run the project, follow these steps:

1. clone the repository and navigate into the project directory, use the following commands:

```bash
git clone https://github.com/Arei1997/GitHubScalaProject.git
cd GitHubScalaProject
```

2. Set up MongoDB:

Make sure you have MongoDB installed and running on your local machine. Adjust the MongoDB connection settings in the application.conf file if necessary.

3. Set up the GitHub API Token:

You'll need to add your GitHub personal access token to the project's configuration. Update the application.conf file with your token:
```bash
github.token = "your-github-personal-access-token"
```

4. Run the project:

Use sbt to run the application:
```bash
sbt run
```

5. Access the application:

Open your web browser and navigate to http://localhost:9000 to access the application.

## Technologies

Technologies
This project leverages the following technologies:

- Scala: The main programming language used for developing the API.
- Play Framework: A powerful web framework for building web applications in Scala.
- MongoDB: A NoSQL database used for storing user data.
- Play WS: A client-side library for making HTTP requests, used to interact with the GitHub API.

## Languages

The following languages are used within the project:

Scala: The primary language for application logic.
HTML: For rendering views.
CSS: For styling the web pages.

## Features

GitHub API Integration: Fetches and displays the top 5 most popular Scala repositories.
User Management: Basic CRUD operations for managing users in MongoDB.
Error Handling: Graceful handling of errors with custom error pages.

## Special Features

Dynamic Repository Fetching: Real-time fetching of popular repositories from GitHub.
User Profile Display: Displays user profiles fetched from GitHub.
File Management: Create, update, and delete files within repositories via the GitHub API.


## Tests

`Add information about the testing framwork`

---
