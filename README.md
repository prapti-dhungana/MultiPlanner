# MultiPlanner
A full-stack web app for planning journeys with multiple stops.
Users can search stations, add and reorder stops, and generate the best route using live TfL data

## Core Features

- **Autocomplete Station Search**
  - Backed by a PostgreSQL NaPTAN dataset (London rail stations only for now)
  - No external API calls for search

- **Multi-stop journey planning**
  - Add, remove, and reorder stops
  - Routes are computed per adjacent leg and combined

- **Routing options**
  - Fastest vs fewest-changes sorting
  - Transport mode filtering (bus, tram; extensible)

- **Journey breakdown**
  - Per-leg summaries
  - Segment-level details (mode, line, direction, duration)

- **Caching for performance**
  - Redis-backed caching for:
    - station lookups
    - TfL journey results (5-minute time buckets)


## Architecture Overview

### Backend (Spring Boot, Java)

- **Layered architecture**
  - `controller` – REST endpoints and request validation
  - `service` – routing logic, TfL integration, optimisation rules
  - `repository` – database access (station search)
  - `client` – external API calls (TfL)
  - `config` – CORS, caching, exception handling

- **design**
  - Station search is DB-backed (NaPTAN), not API-backed
  - Routing is composed of independent leg computations
  - Business logic lives entirely in services
  - Controllers are thin and declarative
  - Global exception handler provides clean API errors

### Frontend (React + TypeScript + Vite)

- Component-based structure:
  - `StationSearch` – debounced autocomplete
  - `RouteOptionsBar` – stylised filters
  - Service layer for API calls and error handling

## Running Locally

This project uses the Transport for London Unified API.
You must create an account to acces their free api key 


### Prerequisites
- Java 21
- Maven
- Node.js (18+ recommended)
- TfL API key
- Docker for Postgres + Redis


### Setup

1. Create a TfL API key  
   https://api-portal.tfl.gov.uk/

2. Configure environment variables at root:
   ```bash
   touch .env
   cp .env.example .env

3. Add your key:
   ```bash
   TFL_APP_KEY=your_key_here

4. Load postgres
   ```bash
   cd infra
   docker compose up -d

4. Start the backend
   ```bash
   cd backend
   mvn spring-boot:run

5. Start the frontened
   ```bash
   npm ci
   npm run dev

### Application
<img width="1175" height="908" alt="image" src="https://github.com/user-attachments/assets/5c62b2fd-2624-46b4-bcd7-2ce4476564cf" />

<img width="593" height="886" alt="image" src="https://github.com/user-attachments/assets/3b641f89-c183-42be-801f-34b859d20514" />

