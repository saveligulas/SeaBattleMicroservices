# Reactive Gateway for Sea Battle Microservices

This is a reactive implementation of the API Gateway for the Sea Battle microservices application using Spring Cloud Gateway. This implementation replaces the MVC-based gateway with a fully reactive, non-blocking gateway.

## Features

- **Reactive Implementation**: Built on Spring WebFlux and Reactor for non-blocking I/O
- **Service Discovery**: Integrates with Eureka for dynamic service discovery
- **Circuit Breaking**: Uses Resilience4j circuit breakers to handle service failures gracefully
- **Routing**: Routes requests to appropriate backend services based on path patterns
- **Error Handling**: Global error handling for consistent error responses
- **Logging**: Comprehensive logging for troubleshooting

## Routes Configuration

The gateway routes requests to three backend services:

1. **Lobby Service**: Handles requests with path pattern `/api/v1/lobby/**`
2. **Game Service**: Handles requests with path pattern `/api/v1/game/**`
3. **Auth Service**: Handles requests with path pattern `/api/v1/auth/**`

Each route includes a circuit breaker configuration to handle service failures gracefully.

## How to Run

1. Make sure Eureka Server is running
2. Start all backend services (auth, game, lobby)
3. Start the reactive-gateway service
4. Access services through the gateway at http://localhost:9090/api/v1/{service}/**

## Advantages over MVC Gateway

- **Performance**: Non-blocking I/O means higher throughput with fewer resources
- **Scalability**: Better handles high concurrency with fewer threads
- **Backpressure**: Built-in support for handling backpressure in reactive streams
- **Resource Efficiency**: More efficient use of system resources

## Circuit Breaker Configuration

Each service has a dedicated circuit breaker with the following configuration:
- Sliding window of 10 requests
- 50% failure threshold
- 10 second wait duration in open state
- 5 permitted calls in half-open state
- 5 second timeout per request
