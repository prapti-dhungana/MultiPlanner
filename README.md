# MultiPlanner
A web application that allows users to plan multi-stop rail journeys and find the fastest, cheapest-estimated, or most reliable routes using real timetable data.

## Why this project?

Most journey planners optimise for a single origin and destination.
This project focuses on journeys involving multiple stops, allowing users to explicitly choose intermediate stations
(e.g. London -> Birmingham -> Manchester -> Liverpool) and compute the best overall itinerary.

The goal is to explore:
- route optimisation across multiple legs
- real-time aware journey planning
- system design patterns used in large-scale transport platforms

## Features

### Core 
- Station search with autocomplete
- Multi-stop journey planning (ordered stops)
- Fastest / cheapest-estimated / fewest-changes sorting
- Journey breakdown by leg
- Cached API responses for performance

### Planned
- Real-time delay and disruption overlays
- Reliability scoring for connections
- Journey re-ranking based on live conditions
- AWS deployment and monitoring


## Repository Structure

- `frontend/` – React + TypeScript web application
- `backend/` – Spring Boot REST API
- `infra/` – Docker and infrastructure configuration
- `.github/` – CI workflows

## Running Locally
This project uses the Transport for London (TfL) Unified API.

1. Create a TfL API key at https://api-portal.tfl.gov.uk/
2. Copy `.env.example` to `.env`
3. Add your key:

   TFL_APP_KEY=your_key_here

4. Start the backend:
   cd backend
   mvn spring-boot:run

4. Start the frontend:
    cd frontend
    npm install
    npm run dev
 

## Project Status

In active development  
Current focus: repository setup, API contracts, and station search MVP.

