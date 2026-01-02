# Runs App - AI-Powered Training Advisor

A Spring Boot application that helps runners make data-driven training decisions based on their current body state (pain level, sleep quality, and stress level).

## Overview

This application transforms running training from "What do I want to run?" to "What does my current body state allow me to run?" by:

- Logging pain level, sleep quality, and stress before each key session
- Using AI-powered logic to adjust training plans based on body state
- Downgrading planned long runs when necessary (e.g., 16 miles → 10 easy miles)
- Recommending cross-training when pain and fatigue spike
- Preventing injury by respecting body signals

## Features

### Body State Tracking
- **Pain Level** (0-10): Track current pain/soreness
- **Sleep Quality** (0-10): Rate last night's sleep
- **Stress Level** (0-10): Track current stress

### Intelligent Training Recommendations

The app calculates an overall body state score and adjusts training accordingly:

- **Excellent (7-10)**: Proceed with planned training
- **Moderate (5-6)**: Downgrade intensity (e.g., long run → easy run at 62.5% distance)
- **Poor (<5)**: Recommend cross-training or recovery runs

#### Example Scenarios

1. **High Pain/Stress**: Recommends cross-training (swimming, cycling, yoga) instead of running
2. **Moderate Fatigue**: Downgrades 16-mile long run to 10-mile easy run
3. **Poor Sleep**: Suggests recovery run (max 3 miles) or rest
4. **Excellent State**: Approves planned training

## Technology Stack

- **Spring Boot 3.2.1**
- **Spring Data JPA** - Database persistence
- **H2 Database** - In-memory database (runtime)
- **Lombok** - Reduce boilerplate code
- **Maven** - Build tool

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Build the Application

```bash
mvn clean package
```

### Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Run Tests

```bash
mvn test
```

## API Endpoints

### Log Body State

```bash
POST /api/body-state
Content-Type: application/json

{
  "painLevel": 7,
  "sleepQuality": 4,
  "stressLevel": 8,
  "notes": "High pain and stress today"
}
```

### Get Latest Body State

```bash
GET /api/body-state/latest
```

### Get All Body States

```bash
GET /api/body-state
```

### Get Training Recommendation

```bash
POST /api/training/recommend
Content-Type: application/json

{
  "plannedType": "LONG_RUN",
  "plannedDistance": 16.0,
  "plannedDate": "2026-01-03T08:00:00"
}
```

**Response Example (Poor Body State):**
```json
{
  "id": 1,
  "plannedType": "LONG_RUN",
  "plannedDistance": 16.0,
  "recommendedType": "CROSS_TRAINING",
  "recommendedDistance": 0.0,
  "recommendation": "Poor body state (Score: 3/10). High pain (7) or stress (8) detected. Recommending cross-training (swimming, cycling, yoga) instead of running to prevent injury.",
  "completed": false
}
```

**Response Example (Moderate Body State):**
```json
{
  "id": 2,
  "plannedType": "LONG_RUN",
  "plannedDistance": 16.0,
  "recommendedType": "EASY_RUN",
  "recommendedDistance": 10.0,
  "recommendation": "Moderate body state (Score: 5/10). Pain: 4, Sleep: 6, Stress: 5. Downgrading from LONG_RUN (16.0 mi) to EASY_RUN (10.0 mi) to protect recovery.",
  "completed": false
}
```

### Get All Training Sessions

```bash
GET /api/training/sessions
```

### Get Session by ID

```bash
GET /api/training/sessions/{id}
```

### Mark Session as Completed

```bash
PUT /api/training/sessions/{id}/complete
```

## Session Types

- `LONG_RUN` - Extended distance run
- `EASY_RUN` - Comfortable pace run
- `TEMPO_RUN` - Sustained effort run
- `INTERVAL_RUN` - High-intensity intervals
- `RECOVERY_RUN` - Very easy recovery run
- `CROSS_TRAINING` - Alternative exercise (swimming, cycling, yoga)
- `REST` - Complete rest day

## Usage Example

```bash
# 1. Log your current body state
curl -X POST http://localhost:8080/api/body-state \
  -H "Content-Type: application/json" \
  -d '{"painLevel": 4, "sleepQuality": 6, "stressLevel": 5, "notes": "Moderate fatigue"}'

# 2. Get training recommendation
curl -X POST http://localhost:8080/api/training/recommend \
  -H "Content-Type: application/json" \
  -d '{"plannedType": "LONG_RUN", "plannedDistance": 16.0, "plannedDate": "2026-01-04T08:00:00"}'

# Result: App recommends 10-mile easy run instead of 16-mile long run
```

## Database

The application uses an in-memory H2 database. You can access the H2 console at:

```
http://localhost:8080/h2-console
```

**Connection details:**
- JDBC URL: `jdbc:h2:mem:runsdb`
- Username: `sa`
- Password: (leave empty)

## Project Structure

```
src/main/java/com/skminfo/runsapp/
├── controller/          # REST controllers
│   ├── BodyStateController.java
│   └── TrainingController.java
├── dto/                 # Data Transfer Objects
│   ├── BodyStateRequest.java
│   └── TrainingSessionRequest.java
├── model/              # Entity models
│   ├── BodyState.java
│   └── TrainingSession.java
├── repository/         # JPA repositories
│   ├── BodyStateRepository.java
│   └── TrainingSessionRepository.java
├── service/            # Business logic
│   ├── BodyStateService.java
│   └── TrainingAdvisorService.java
└── RunsAppApplication.java
```

## Algorithm Details

The AI-powered advisor uses a weighted scoring system:

1. **Pain Score** = 10 - painLevel
2. **Sleep Score** = sleepQuality
3. **Stress Score** = 10 - stressLevel
4. **Overall Score** = (Pain Score + Sleep Score + Stress Score) / 3

Based on the overall score:
- **≥7**: Excellent - proceed as planned
- **5-6**: Moderate - adjust intensity/distance
- **<5**: Poor - recommend cross-training or recovery

Special rules:
- Pain ≥7 or Stress ≥8 → Force cross-training
- Long runs in moderate state → Reduce to 62.5% distance as easy run
- Intensity sessions in moderate state → Convert to easy runs at 80% distance

## License

This project is licensed under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
