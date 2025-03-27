# SeaBattle Microservices Guide

## Setup

Open project in terminal and run: ```docker-compose up```

If everything worked correctly you should have a running neo4j and redis instance running on the standard ports.

Neo4j can be looked at in browser: ```http://localhost:7474/browser/```

Username: neo4j

Password: Reader@123

## Architecture Overview

This project implements a sea battle game (similar to Battleship) using a microservices architecture. The system consists of the following components:

1. **Gateway Service (port 9090)**: Entry point for all requests
2. **Authorization Service (port 9093)**: Handles user registration and authentication
3. **Lobby Service (port 9092)**: Manages game lobbies and player matching
4. **Game Service (port 9091)**: Implements the core game logic

## Authentication

The system uses a simple header-based authentication mechanism:

1. First, register a user ID using the authorization service
2. Include the registered User-ID in the request header for all protected endpoints
3. The gateway validates this User-ID with the authorization service

## Microservices Endpoints

All endpoints are accessed through the gateway by appending: `/api/v1/{microservice}/{endpoint}`

| Microservice | Endpoint | Method | Description | Authentication Required | Gateway URL | Request Body/Params |
|-------------|----------|--------|-------------|------------------------|-------------|--------------------|
| **Authorization** | `/register` | POST | Register a new user | No | `/api/v1/auth/register?userId={userId}` | Query param: userId |
| **Authorization** | `/validate` | GET | Validate a user token | No | `/api/v1/auth/validate?userId={userId}` | Query param: userId |
| **Lobby** | `/lobby` | POST | Create a new game lobby | No | `/api/v1/lobby/lobby` | None |
| **Lobby** | `/lobby/{id}` | POST | Join an existing lobby | No | `/api/v1/lobby/lobby/{id}?userId={userId}` | Query param: userId |
| **Lobby** | `/lobby/{id}/start` | POST | Start a game from lobby | No | `/api/v1/lobby/lobby/{id}/start` | None |
| **Game** | `/game` | POST | Create a new game | Yes | `/api/v1/game/game` | `{"redPlayer":"player1","bluePlayer":"player2","shipSizes":[2,3,4]}` |
| **Game** | `/game/{id}` | GET | Get game info | Yes | `/api/v1/game/game/{id}` | None |
| **Game** | `/game/{id}/setup` | POST | Set up ships on the map | Yes | `/api/v1/game/game/{id}/setup` | Header: player-id=RED or BLUE, Body: `{"ships":[[0,1],[2,3,4]]}` |
| **Game** | `/game/{id}/phase` | GET | Get current game phase | Yes | `/api/v1/game/game/{id}/phase` | None |
| **Game** | `/game/{id}/map/opponent` | GET | View opponent's map | Yes | `/api/v1/game/game/{id}/map/opponent` | Header: player-id=RED or BLUE |
| **Game** | `/game/{id}/fire` | POST | Fire a shot | Yes | `/api/v1/game/game/{id}/fire` | Header: player-id=RED or BLUE, Body: `{"x":0,"y":0}` |
| **Game** | `/game/{id}/turn` | GET | Get whose turn it is | Yes | `/api/v1/game/game/{id}/turn` | None |
| **Game** | `/game/{id}/winner` | GET | Get the winner (if any) | Yes | `/api/v1/game/game/{id}/winner` | None |

## How to Access the API

### Step 1: Register a User

```
POST http://localhost:9090/api/v1/auth/register?userId=player1
```

### Step 2: Use Authentication for Protected Endpoints

For any endpoint requiring authentication, add the header:

```
User-ID: player1
```

### Step 3: Access the Game Flow

1. Create a lobby: `POST /api/v1/lobby/lobby`
2. Join the lobby with two players: `POST /api/v1/lobby/lobby/{id}?userId=player1`
3. Start a game: `POST /api/v1/lobby/lobby/{id}/start`
4. Set up ships for both players: `POST /api/v1/game/game/{id}/setup`
5. Play the game by firing shots: `POST /api/v1/game/game/{id}/fire`

## Swagger UI

All endpoints can also be accessed and tested through the Swagger UI of the gateway application at:

```
http://localhost:9090/swagger-ui/index.html
```

Note that you may still need to manually add the User-ID header for protected endpoints.