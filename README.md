# MultiPlanner

A full-stack web application for planning multi-stop rail journeys with an emphasis on route optimisation.

Users can search stations, add and reorder intermediate stops, and compute the best overall itinerary using live Transport for London (TfL) routing data.



## Why this project?

Most journey planners optimise for a single origin to destination model.
However, in reality, many journeys can involve many intermediate stops (e.g. meeting someone or planning a trip).

MultiPlanner focuses on:
- explicit multi-stop routing
- reasoning about journeys across multiple legs
- modelling how large transport platforms structure routing, caching, and APIs

The project is primarily an exploration of system design and backend architecture, rather than just UI.


## Core Features

- **Station search with autocomplete**
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

- **Key design decisions**
  - Station search is DB-backed (NaPTAN), not API-backed
  - Routing is composed of independent leg computations
  - Business logic lives entirely in services
  - Controllers are thin and declarative
  - Global exception handler provides clean API errors

- **What I did not use**
  - No JPA / Hibernate (explicit SQL via JdbcTemplate for clarity)
  - No frontend-side routing logic
  - No external station lookup APIs


### Frontend (React + TypeScript + Vite)

- Component-based structure:
  - `StationSearch` – debounced autocomplete
  - `RouteOptionsBar` – Google-Maps-style filters
  - Service layer for API calls and error handling

- UI intentionally simple and functional
  - Focus is correctness, data flow, and UX clarity
  - Errors are surfaced clearly from backend responses


## Running Locally

This project uses the Transport for London Unified API.
You must create an account to acces their free api key 


### Prerequisites
- Java 21
- Maven
- Node.js (18+ recommended)
- TfL API key
-  Docker for Postgres + Redis


### Setup

1. Create a TfL API key  
   https://api-portal.tfl.gov.uk/

2. Configure environment variables:
   - Create a `.env` file
   ```bash
   cp .env.example .env

3. Add your key:
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
