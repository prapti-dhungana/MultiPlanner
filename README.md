# MultiPlanner
MultiPlanner is a full-stack web application for planning journeys with multiple stops across London.
Users can search stations, add and reorder stops, and generate an optimal route using live Transport for London (TfL) journey data.


## Core Features

- **Autocomplete Station Search**
  - Backed by a PostgreSQL NaPTAN dataset (London rail stations only for now)
  - No external API calls for search

- **Multi-stop journey planning**
  - Add, remove, and reorder stops
  - Routes are computed per adjacent leg and combined

- **Routing options**
  - Sort by fastest or fewest changes
  - Transport mode filtering (bus, tram)
  - Designed to be easily extensible to additional modes

- **Journey breakdown**
  - Per-leg summaries
  - Segment-level details (transport mode, line, direction, duration)

- **Caching for performance**
  - Redis-backed caching for:
    - station lookups
    - TfL journey results (5-minute time buckets)
  - Reduces API calls and improves response times


## Architecture Overview

### Backend (Spring Boot, Java)
- **Layered architecture**
  - `controller` – REST endpoints and request validation
  - `service` – routing logic, TfL integration and optimisation rules
  - `repository` – database access (station search)
  - `client` – external API calls (TfL)
  - `config` – caching and exception handling

- **design decisions**
  - Station search is DB-backed (NaPTAN), not API-backed
  - Routing is composed of independent leg computations
  - All business logic lives in the service layer
  - Controllers are thin and declarative
  - Global exception handler provides clean API errors

### Frontend (React + TypeScript + Vite)
- Component-based design
- Key Components:
  - `StationSearch` – debounced autocomplete
  - `RouteOptionsBar` – stylised filters
  - Service layer for API calls and error handling
- Dedicated service layer for API calls and error handling
- Frontend served via Nginx in production

# Running Locally
The application is designed to be easy to run with Docker.

### Prerequisites
- Docker & Docker Compose
- TfL API key (free)

### Setup
1.  Register for an API Key here:
   https://api-portal.tfl.gov.uk/

2. Configure the API Key
   - At project root, copy contents of .env_example into (newFile) .env
   - e.g. on linux:
   ```bash
   touch .env
   cp .env_example .env

3. Add your key into .env:
   ```bash
   TFL_APP_KEY=your_key_here

4. Start the Application:
   ```bash
   docker compose up --build

- View Frontend: http://localhost:8080
- Backend API: proxied internally via /api/*
- Postgres and Redis are fully containerised
- No local Java, Node, Postgres, or Redis installs required.


### Screenshots
<img width="1175" height="908" alt="image" src="https://github.com/user-attachments/assets/5c62b2fd-2624-46b4-bcd7-2ce4476564cf" />

<img width="593" height="886" alt="image" src="https://github.com/user-attachments/assets/3b641f89-c183-42be-801f-34b859d20514" />

