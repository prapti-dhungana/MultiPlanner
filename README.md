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
> Setup instructions will be added once the initial backend and frontend scaffolding is complete.

## Project Status

In active development  
Current focus: repository setup, API contracts, and station search MVP.

