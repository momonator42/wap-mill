# WAP-Mill (Stateless Game Logic Microservice)


## Overview
WAP-Mill is a stateless microservice that handles the game logic for Mühle (Nine Men's Morris). It enforces rules and validates moves, while state management is left to external services.

## Download and Setup


## Requirements
-   **Heroku CLI** (for deployment)

### 1. Download the project by cloning the repository:

```bash
git clone https://github.com/momonator42/wap-webserver.git
```

### 2. Navigate into the project directory:

```bash
cd wap-webserver
```

### 3. Build the project using sbt:

```bash
sbt compile
```

### 4. Set the environemnt variable for the [wap-webserver](https://github.com/momonator42/wap-webserver):
```bash
$env:JWT_SECRET = "<your-password>"
```

### 4. Run the service:

```bash
sbt run
```

## Docker Setup

### Assemble the project:
```bash
sbt assembly
```

### Build and run with Docker:
```bash
docker build -t wap-mill .
docker run -p 9000:9000 wap-mill
```

## Deployment

### 1. assemble the project:
```bash
sbt assembly
```

### 2. push in your heroku:
```bash
heroku container:push web --a <app-name> 
```

### 3. set the environment variable:
```bash
heroku config:set JWT_SECRET=<your-password> -a <app-name>
```

### 3. release it and enjoy
```bash
heroku container:release web --app <app-name>
```



