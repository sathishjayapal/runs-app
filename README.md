# Runs App - Smart Boston Marathon Coach

A smart running coach built with Spring Boot that helps train for the Boston Marathon while monitoring health metrics and refusing to push when your body shows red flags.

## Overview

This application demonstrates responsible athletic training by:
- Providing personalized coaching advice based on training context
- Monitoring health metrics (heart rate, sleep, stress, injuries, fatigue)
- **Refusing to push athletes when red flags are detected**
- Prioritizing long-term health over short-term gains

> **Note**: This implementation uses a rule-based coaching engine. The architecture is designed to easily integrate with Spring AI (OpenAI GPT-4 or other LLMs) for AI-powered coaching advice in the future.

## Features

- **Smart Coaching**: Provides context-aware training advice based on training phase, mileage, and race proximity
- **Health Monitoring**: Tracks multiple health metrics including:
  - Resting heart rate
  - Sleep hours
  - Stress levels
  - Injury status
  - Fatigue levels
  - Recovery time
- **Red Flag Detection**: Automatically identifies concerning health indicators
- **Smart Refusal**: Coach refuses intense training recommendations when health metrics show warning signs
- **REST API**: Easy-to-use API for integration with other applications

## Prerequisites

- Java 17 or higher
- Maven 3.6+

## Configuration

The application uses Spring Boot's standard configuration in `application.properties`. No additional configuration is required for basic usage.

## Building and Running

### Build the application
```bash
mvn clean package
```

### Run the application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Usage

### Health Check
```bash
curl http://localhost:8080/api/coach/health
```

### Get Coaching Advice (No Red Flags)
```bash
curl -X POST http://localhost:8080/api/coach/advice \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Should I do a 20-mile long run this weekend?",
    "healthMetrics": {
      "heartRate": 65,
      "sleepHours": 8.0,
      "stressLevel": 3,
      "hasInjury": false,
      "feelingFatigued": false,
      "recoveryDays": 2
    },
    "trainingContext": {
      "currentPhase": "peak",
      "weeksUntilRace": 4,
      "currentWeeklyMileage": 50.0,
      "lastWorkoutType": "tempo run",
      "trainingForBoston": true
    }
  }'
```

### Get Coaching Advice (With Red Flags)
```bash
curl -X POST http://localhost:8080/api/coach/advice \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Should I do a hard interval workout today?",
    "healthMetrics": {
      "heartRate": 105,
      "sleepHours": 5.0,
      "stressLevel": 8,
      "hasInjury": true,
      "feelingFatigued": true,
      "recoveryDays": 0
    },
    "trainingContext": {
      "currentPhase": "build",
      "weeksUntilRace": 8,
      "currentWeeklyMileage": 45.0,
      "trainingForBoston": true
    }
  }'
```

## Response Format

```json
{
  "advice": "Detailed coaching advice based on your metrics and context",
  "allowWorkout": false,
  "reason": "Your body is showing warning signs. Rest and recovery are essential.",
  "redFlags": "Active injury; Elevated resting heart rate (105 bpm); Insufficient sleep (5.0 hours); High stress level (8/10); Feeling fatigued; Insufficient recovery time;"
}
```

## Red Flag Criteria

The coach detects red flags based on:
- **Heart Rate**: Resting heart rate > 100 bpm
- **Sleep**: Less than 6 hours
- **Stress**: Level > 7 on a 1-10 scale
- **Injury**: Any active injury
- **Fatigue**: Feeling fatigued
- **Recovery**: Less than 1 day since last workout

When red flags are detected, `allowWorkout` is set to `false` and the coach provides recovery-focused advice instead of training recommendations.

## Testing

Run the tests:
```bash
mvn test
```

The test suite includes:
- Unit tests for health metrics red flag detection
- Spring Boot context loading tests

## Architecture

- **Spring Boot 3.2.1**: Application framework
- **Lombok**: Reduces boilerplate code
- **Maven**: Build and dependency management
- **Rule-based coaching engine**: Provides intelligent, context-aware coaching advice

### Key Components

- `CoachService`: Core service that processes coaching requests and generates advice
- `HealthMetrics`: Model for tracking athlete health indicators with red flag detection
- `CoachController`: REST API endpoints
- `CoachingRequest/Response`: Request/response models
- `TrainingContext`: Training phase and progress information

## Future Enhancements

The application is architected to support:
- Integration with Spring AI for LLM-powered coaching
- Mobile app integration
- Training plan generation
- Workout tracking and history
- Personalized race-day pacing strategies

## Philosophy

This application embodies a responsible approach to athletic training:
1. **Health First**: Always prioritizes athlete well-being over performance goals
2. **Transparent Reasoning**: Clearly explains why certain recommendations are made
3. **Refuses When Necessary**: Won't push athletes when body shows warning signs
4. **Personalized**: Takes into account individual context and metrics
5. **Evidence-Based**: Based on established training principles and injury prevention research

## License

This project is for demonstration purposes.

## Acknowledgments

Inspired by the question: "Can I use Spring Boot and Spring AI to build a coach that refuses to push me when my body is seeing red flags?"

This application demonstrates the core concept of health-aware coaching that prioritizes athlete safety over aggressive training.
