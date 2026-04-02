# CWUNI_Clientside

**Module:** 5COSC022W.2 – Client-Server Architectures  
**University:** University of Westminster  
**NAME:** Jamie Buttigieg
**ID:** W2077449
 

# Smart Campus Sensor & Room Management API

## Overview

This project is a RESTful API built with JAX-RS (Jersey 3.x) and an embedded Grizzly HTTP server for the University of Westminster's Smart Campus initiative. It manages Rooms, Sensors, and Sensor Readings using only in-memory data structures (ConcurrentHashMap and ArrayList) — no database is used.

The API is versioned and available at:
http://localhost:8080/api/v1/


### Resource Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1/ | Discovery – API metadata and links |
| GET | /api/v1/rooms | List all rooms |
| POST | /api/v1/rooms | Create a new room |
| GET | /api/v1/rooms/{roomId} | Get a specific room |
| DELETE | /api/v1/rooms/{roomId} | Delete a room (blocked if sensors exist) |
| GET | /api/v1/sensors | List all sensors (supports ?type= filter) |
| POST | /api/v1/sensors | Create a sensor (validates roomId exists) |
| DELETE | /api/v1/sensors/{sensorId} | Delete a sensor |
| GET | /api/v1/sensors/{sensorId}/readings | Get reading history for a sensor |
| POST | /api/v1/sensors/{sensorId}/readings | Add a new reading (updates currentValue) |

### Data Models

**Room**
```json
{
  "id": "LIB-301",
  "name": "Library Quiet Study",
  "capacity": 40,
  "sensorIds": ["TEMP-001"]
}
```

**Sensor**
```json
{
  "id": "TEMP-001",
  "type": "Temperature",
  "status": "ACTIVE",
  "currentValue": 22.5,
  "roomId": "LIB-301"
}
```

**SensorReading**
```json
{
  "id": "uuid-generated",
  "timestamp": 1712000000000,
  "value": 22.5
}
```

---

## How to Build and Run

### Prerequisites

- Java 17
- NetBeans 18 (or any Maven-compatible IDE)
- Maven (bundled with NetBeans 18)

### Steps

1. Clone the repository or download:

   ```bash
   git clone https://github.com/YOURUSERNAME/CWUNI_Clientside.git
   ```

2. Open the project in NetBeans 18:
   - File → Open Project → select the cloned folder.

3. Reload Maven dependencies:
   - Right-click the project → Maven → Reload Project.

4. Set the main class:
   - Right-click the project → Properties → Run.
   - Set Main Class to `uk.ac.westminster.smartcampus.Main`.

5. Clean and Build:
   - Right-click the project → Clean and Build.

6. Run the server:
   - Press F6 or right-click → Run.
   - You should see in the Output window:

     ```
     INFO: Started listener bound to [0.0.0.0:8080]
     INFO: Smart Campus API started at http://0.0.0.0:8080/api/v1/
     INFO: Press Ctrl+C to stop the server.
     ```

7. Verify in a browser:
   - Open `http://localhost:8080/api/v1/`
   - You should see the discovery JSON response.

> **Important:** All data is stored in memory. Every time the server restarts, all data is cleared. You must re-create rooms before creating sensors linked to them.

---

## Sample curl Commands

All commands use bash syntax (Git Bash / WSL / macOS / Linux). Ensure the server is running before executing.

### 1. Create a room

```bash
curl -X POST "http://localhost:8080/api/v1/rooms" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "LIB-301",
    "name": "Library Quiet Study",
    "capacity": 40
  }'
```

Expected: `201 Created` with the room JSON.

### 2. List all rooms

```bash
curl -X GET "http://localhost:8080/api/v1/rooms" \
  -H "Accept: application/json"
```

Expected: `200 OK` with a JSON array of all rooms.

### 3. Create a sensor linked to a room

```bash
curl -X POST "http://localhost:8080/api/v1/sensors" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 0.0,
    "roomId": "LIB-301"
  }'
```

Expected: `201 Created` with the sensor JSON.

### 4. Filter sensors by type

```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature" \
  -H "Accept: application/json"
```

Expected: `200 OK` with an array containing only Temperature sensors.

### 5. Add a reading to a sensor

```bash
curl -X POST "http://localhost:8080/api/v1/sensors/TEMP-001/readings" \
  -H "Content-Type: application/json" \
  -d '{
    "value": 22.5
  }'
```

Expected: `201 Created` with the reading JSON. The sensor's `currentValue` is updated to `22.5`.

### 6. Delete a sensor

```bash
curl -X DELETE "http://localhost:8080/api/v1/sensors/TEMP-001"
```

Expected: `204 No Content`.

### 7. Delete a room

```bash
curl -X DELETE "http://localhost:8080/api/v1/rooms/LIB-301"
```

Expected: `204 No Content` if no sensors are assigned.  
If sensors still exist: `409 Conflict` with a JSON error body.

---

## Conceptual Report – Answers to Specification Questions

---

### Part 1 – Service Architecture & Setup

#### Question 1.1 – JAX-RS Resource Class Lifecycle

By default, JAX-RS creates a **new instance of a resource class for every incoming HTTP request** (per-request scope). This means each request gets its own fresh object, and any instance variables are not shared between requests.

This has a direct impact on in-memory data management. If each request created a new resource instance with its own data structure, all stored data would be lost between requests. To prevent this, the data store must be managed outside the resource class lifecycle using the **Singleton pattern**. In this implementation, `InMemoryDataStore` uses `getInstance()` to return a single shared instance backed by a `ConcurrentHashMap`. This ensures all resource instances — created fresh per request — share the same underlying data. `ConcurrentHashMap` is specifically chosen over a standard `HashMap` because it provides thread-safe read and write operations, preventing race conditions when multiple requests arrive simultaneously and attempt to modify the same data structure concurrently.

#### Question 1.2 – HATEOAS and Hypermedia Links

HATEOAS (Hypermedia as the Engine of Application State) means API responses include hyperlinks to related resources, allowing clients to navigate the API dynamically rather than relying on hardcoded URLs or external static documentation.

This benefits client developers in several important ways. First, it reduces tight coupling between client and server — if the server changes a URL structure, clients that follow links rather than hardcoding paths do not need to be updated. Second, it makes the API self-documenting at runtime; a developer can start at the root discovery endpoint (`GET /api/v1/`) and find all available resources without reading a separate manual. Third, it aligns with how the web itself works — following links rather than memorising addresses. Compared to static documentation, which can become outdated and requires manual maintenance, hypermedia responses are always current because they are generated by the live server. This is considered a hallmark of mature REST design because it gives clients everything they need to interact with the API from a single entry point.

---

### Part 2 – Room Management

#### Question 2.1 – Returning IDs vs Full Room Objects

Returning **only IDs** in a list response reduces payload size significantly, which benefits performance on large datasets or slow network connections. However, it forces the client to make a separate `GET /rooms/{id}` request for every room it needs details about, causing the N+1 problem — one request for the list, plus one per room — which increases latency and server load dramatically at scale.

Returning **full room objects** increases the size of each response but gives the client everything it needs in a single request, eliminating unnecessary round-trips. For this campus API, where room objects are lightweight and clients managing facilities typically need full details immediately, returning complete objects in the list is the better trade-off. It reduces client-side complexity and the number of HTTP calls needed to render a full room listing.

#### Question 2.2 – Idempotency of DELETE

Yes, the DELETE operation is idempotent in this implementation. HTTP idempotency means that sending the same request multiple times produces the same server state as sending it once.

In this API, if a client sends `DELETE /rooms/LIB-301` and the room exists with no sensors, it is deleted and `204 No Content` is returned. If the exact same request is sent again, the room no longer exists in the `ConcurrentHashMap`, so the method detects `room == null` and returns `204 No Content` again rather than `404 Not Found`. The end state after each call is identical — the room does not exist — regardless of how many times the request is made. This satisfies the idempotency contract defined by the HTTP specification. The design choice to return `204` rather than `404` on a repeated delete is deliberate, as it prevents clients from having to distinguish between "deleted just now" and "was already deleted," simplifying retry logic in automated systems.

---

### Part 3 – Sensor Operations & Linking

#### Question 3.1 – Effect of Mismatched Content-Type on POST

When a client sends a POST request with a `Content-Type` header that does not match `application/json` — for example `text/plain` or `application/xml` — JAX-RS automatically rejects the request before it reaches the resource method. The framework's content negotiation mechanism compares the incoming `Content-Type` against the value declared in `@Consumes(MediaType.APPLICATION_JSON)`. If there is no match, the JAX-RS runtime returns **HTTP 415 Unsupported Media Type** to the client.

This protection is handled entirely by the runtime without any manual checking required in the resource method. The `@Consumes` annotation acts as a strict contract gate: only requests with the correct media type are allowed through to the business logic. This prevents the server from attempting to deserialise incompatible data formats, which would otherwise cause parsing errors or unexpected behaviour. It also enforces a clear API contract, making it immediately obvious to clients what format is required.

#### Question 3.2 – Query Parameter vs Path Segment for Filtering

Using a query parameter (`GET /sensors?type=CO2`) is the correct REST approach for filtering because it communicates that `type` is an optional, variable search criterion applied to the base `/sensors` collection — not a distinct addressable resource in its own right.

Using a path segment (`GET /sensors/type/CO2`) would imply that `type/CO2` is a unique resource, which is semantically incorrect. Path parameters are designed to identify specific resources (e.g., `/sensors/TEMP-001` identifies a specific sensor). Query parameters are designed to modify or filter the result of a resource retrieval. Additionally, query parameters are far more flexible: multiple filters can be combined naturally (`?type=CO2&status=ACTIVE`) without requiring a complex or deeply nested URL path. They are also universally recognised by HTTP tooling, caching layers, and API consumers as filtering mechanisms, making the API more intuitive, consistent with web standards, and easier to extend in future.

---

### Part 4 – Sub-Resources

#### Question 4.1 – Benefits of the Sub-Resource Locator Pattern

The sub-resource locator pattern delegates responsibility for a nested path to a dedicated class. In this API, `SensorResource` contains a locator method annotated with `@Path("{sensorId}/readings")` that returns an instance of `SensorReadingResource`, which handles all reading-specific operations.

The architectural benefits are significant:

1. **Separation of concerns** – Each class has a single, clearly defined responsibility. `SensorResource` manages sensors; `SensorReadingResource` manages readings. This makes each class shorter, easier to read, test independently, and maintain.

2. **Reduced complexity** – In a large API, defining every nested route inside one controller results in a class with hundreds of methods, making it extremely difficult to navigate and maintain. Sub-resource locators allow the route tree to be split logically across multiple focused files.

3. **Parent validation in one place** – The locator method validates the parent resource (the sensor exists) before returning the sub-resource instance. This means every reading operation automatically has a verified sensor context, rather than each reading method needing to repeat that lookup.

4. **Scalability** – As the API grows, new sub-resources can be added by creating new classes and wiring them through locators, without modifying existing controllers. This aligns with the Open/Closed Principle — open for extension, closed for modification.

---

### Part 5 – Advanced Error Handling & Logging

#### Question 5.1 – Why HTTP 422 is More Semantically Accurate than 404

When a client POSTs a new sensor with a `roomId` that does not exist, the **request URL is valid** and the **JSON payload is syntactically correct** — the problem is semantic: a field inside the payload references a resource that cannot be found.

HTTP 404 (Not Found) is specifically intended to signal that the **requested URL itself** does not exist on the server. Using 404 here would mislead the client into thinking the `/sensors` endpoint is missing, which is false.

HTTP 422 (Unprocessable Entity) explicitly means the server understands the request format and the URL is valid, but it cannot process the request because of a **semantic error in the content**. This precisely describes the scenario: the JSON is well-formed, but the value of `roomId` is not meaningful in the current system state. Returning 422 allows clients and automated systems to distinguish cleanly between a wrong URL (404) and a logically invalid reference inside an otherwise correct request (422), enabling more targeted and accurate error handling.

#### Question 5.2 – Security Risks of Exposing Java Stack Traces

Exposing raw Java stack traces to external API consumers is a serious security risk for several reasons:

1. **Framework and library version disclosure** – Stack traces reveal the exact class names and versions of frameworks in use (e.g., Jersey, Grizzly, Jackson). Attackers can cross-reference these with public CVE vulnerability databases to identify known, unpatched security flaws to exploit.

2. **Internal architecture exposure** – Package names, class names, and method signatures reveal the internal structure of the application, effectively providing a partial map of the codebase to an attacker without any source code access.

3. **File path and system information disclosure** – Stack traces often include absolute file paths on the server (e.g., `C:\Users\...`), revealing the operating system type, username, directory structure, and deployment layout.

4. **Business logic leakage** – Method names and call chains can reveal how security checks, validation, and data access are structured, potentially exposing areas where those checks are absent or can be bypassed.

5. **Targeted attack facilitation** – With knowledge of specific class names and line numbers, an attacker can craft highly targeted payloads designed to trigger specific code paths or known vulnerabilities.

The `ExceptionMapper<Throwable>` in this implementation intercepts all unexpected exceptions and returns only a safe, generic `500 Internal Server Error` JSON response, ensuring no internal details are ever exposed to external consumers.

#### Question 5.3 – Benefits of JAX-RS Filters for Cross-Cutting Concerns

Using a JAX-RS filter that implements both `ContainerRequestFilter` and `ContainerResponseFilter` for logging is far superior to inserting `Logger.info()` calls manually inside every resource method for the following reasons:

1. **Single point of maintenance** – If the logging format needs to change (e.g., adding a timestamp or correlation ID), only the filter class needs updating. With manual logging, every resource method across the entire codebase would need to be changed individually, which is error-prone and time-consuming.

2. **Guaranteed coverage** – A filter intercepts every single request and response automatically, including requests that are rejected before they even reach a resource method (e.g., 415 Unsupported Media Type, 404 Not Found). Manual logging inside resource methods would completely miss these cases, creating blind spots in observability.

3. **Separation of concerns** – Resource methods should contain only business logic. Mixing logging statements into them violates the Single Responsibility Principle and makes the code harder to read, understand, and test. The filter cleanly separates the cross-cutting concern of logging from the core application logic.

4. **No code duplication** – With dozens of endpoints across multiple resource classes, manually writing log statements in each one produces a large amount of repetitive boilerplate code. The filter achieves the same result with a single class registered once with the JAX-RS runtime.

5. **Consistency** – Filters guarantee that every log entry follows exactly the same format and contains the same fields (method, URI, status code). Manually written log statements tend to vary in format across different developers and methods, making log analysis, monitoring, and debugging significantly harder in production.

------------------------------END---------------------------