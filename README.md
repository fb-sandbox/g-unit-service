# Unit Service

A REST microservice for managing Units (vehicles/equipment) built with Quarkus on AWS Lambda and DynamoDB. This service demonstrates serverless patterns with immutable architecture principles.

## Overview

The Unit Service is a proof-of-concept microservice that provides REST APIs for CRUD operations on Unit resources. It runs on AWS Lambda with Quarkus framework and uses DynamoDB for persistent storage. The service is optimized for Lambda SnapStart to reduce cold start times.

### Key Features

- **REST API** for Unit management with query parameters for filtering
- **DynamoDB Integration** with Global Secondary Indexes for flexible querying
- **Immutable Architecture** - all data structures follow "No Exceptions" immutability principle
- **XRay Tracing** for distributed tracing and monitoring
- **AWS Lambda Optimization** with SnapStart support
- **Validation** for incoming requests with detailed error responses
- **Comprehensive Tests** with 19 passing unit tests

## Tech Stack

- **Java 21** - Latest LTS version
- **Quarkus** - Ultra-lightweight Java framework optimized for Lambda
- **AWS Lambda** - Serverless compute
- **Amazon DynamoDB** - NoSQL database
- **AWS SDK v2 Enhanced Client** - Type-safe DynamoDB access
- **Lombok** - Code generation for boilerplate elimination
- **Jakarta/CDI** - Dependency injection
- **JAX-RS** - REST endpoints
- **JUnit 5** - Unit testing
- **Mockito** - Test mocking

## Project Structure

```
unit-service/
├── build.gradle                                 # Gradle build configuration
├── gradle.properties                            # Gradle properties
├── settings.gradle                              # Gradle multi-project settings
├── Taskfile.yml                                 # Task definitions for build automation
├── README.md                                    # This file
├── src/
│   ├── main/
│   │   ├── java/com/fullbay/unit/
│   │   │   ├── LambdaFunctionHandler.java      # Lambda entry point
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── DynamoDbConfig.java         # DynamoDB client and table configuration
│   │   │   │   └── StartupConfig.java          # Application startup initialization
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── entity/
│   │   │   │   │   └── UnitEntity.java         # DynamoDB entity (immutable @Value)
│   │   │   │   ├── dto/
│   │   │   │   │   ├── UnitDto.java            # API response DTO
│   │   │   │   │   ├── CreateUnitRequest.java  # Create request with validation
│   │   │   │   │   └── UpdateUnitRequest.java  # Update request with optional fields
│   │   │   │   └── response/
│   │   │   │       ├── ApiResponse.java        # Generic success/error wrapper
│   │   │   │       ├── ErrorDetail.java        # Error information
│   │   │   │       └── ValidationError.java    # Individual field validation error
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   └── UnitRepository.java         # DynamoDB data access layer
│   │   │   │
│   │   │   ├── service/
│   │   │   │   └── UnitService.java            # Business logic layer
│   │   │   │
│   │   │   ├── resource/
│   │   │   │   └── UnitResource.java           # REST endpoints (@Path "/units")
│   │   │   │
│   │   │   ├── mapper/
│   │   │   │   └── UnitMapper.java             # Entity <-> DTO conversions (static utility)
│   │   │   │
│   │   │   ├── util/
│   │   │   │   └── IdGenerator.java            # Unit ID generation utility
│   │   │   │
│   │   │   └── exception/
│   │   │       ├── UnitNotFoundException.java   # 404 exception
│   │   │       ├── DuplicateVinException.java   # 409 exception
│   │   │       └── GlobalExceptionHandler.java  # JAX-RS exception mapping
│   │   │
│   │   └── resources/
│   │       └── application.properties          # Quarkus configuration
│   │
│   └── test/
│       ├── java/com/fullbay/unit/
│       │   ├── util/
│       │   │   └── IdGeneratorTest.java        # ID generation tests
│       │   ├── mapper/
│       │   │   └── UnitMapperTest.java         # Entity/DTO mapping tests
│       │   ├── service/
│       │   │   └── UnitServiceTest.java        # Business logic tests
│       │   └── resource/
│       │       └── UnitResourceTest.java.integration  # REST integration tests (disabled)
│       │
│       └── resources/
│           └── application-test.properties    # Test-specific configuration
│
├── terraform/
│   ├── main.tf                                # DynamoDB table, Lambda, IAM resources
│   ├── variables.tf                           # Terraform variables
│   ├── locals.tf                              # Local values and common tags
│   ├── provider.tf                            # AWS provider configuration
│   └── outputs.tf                             # Output values (table name, Lambda ARN)
│
└── sherpa/
    ├── sherpa.yml                             # Deployment configuration
    └── runtime-config.yml                     # Runtime settings
```

## Architecture

### Layered Architecture

The service follows a clean, layered architecture:

```
REST Resource (UnitResource)
        ↓
    Service Layer (UnitService)
        ↓
  Repository Layer (UnitRepository)
        ↓
   DynamoDB Enhanced Client
        ↓
   DynamoDB Service
```

### Data Flow

1. **Request** → REST endpoint validates input
2. **Service** → Business logic executes (ID generation, duplicate checks, etc.)
3. **Repository** → Data access operations with XRay tracing
4. **DynamoDB** → Persistence layer
5. **Response** → Wrapped in ApiResponse<T> with consistent format

### Immutability Principle

The codebase strictly follows "Be Immutable. No Exceptions!":

- **Data Classes**: All DTOs and entities use `@Value` (immutable) with `@Builder`
- **Update Pattern**: Uses `@Wither` for safe copying with field updates
- **Local Variables**: All non-reassigned variables are marked `final` (31 variables)
- **No Setters**: No setter methods anywhere in the codebase
- **Final Fields**: All instance fields are `final`

## REST API Endpoints

Base path: `/v1/units`

### List Units

```bash
GET /v1/units
GET /v1/units?customerId=cst-123
GET /v1/units?vin=1HGCM82633A004352
```

**Response (Success):**
```json
{
  "data": {
    "items": [
      {
        "unitId": "unt-abc1234",
        "customerId": "cst-123",
        "vin": "1HGCM82633A004352",
        "year": 2020,
        "make": "Honda",
        "model": "Accord",
        ...
      }
    ],
    "count": 1
  }
}
```

### Get Single Unit

```bash
GET /v1/units/{unitId}
```

**Response (Success):**
```json
{
  "data": {
    "unitId": "unt-abc1234",
    "customerId": "cst-123",
    "vin": "1HGCM82633A004352",
    ...
  }
}
```

**Response (Not Found):**
```json
{
  "data": null,
  "error": {
    "code": "UNIT_NOT_FOUND",
    "message": "Unit with id unt-invalid not found",
    "details": []
  }
}
```

### Create Unit

```bash
POST /v1/units
Content-Type: application/json

{
  "customerId": "cst-123",
  "vin": "1HGCM82633A004352",
  "year": 2020,
  "make": "Honda",
  "model": "Accord",
  "submodel": "Sedan",
  "engineType": "Gas",
  "transmissionType": "Automatic",
  "driveType": "FWD",
  "unitType": "TRUCK",
  "attributes": {
    "color": "Blue",
    "mileage": 45000
  }
}
```

**Response:** 201 Created with Unit object

**Response (Duplicate VIN):**
```json
{
  "data": null,
  "error": {
    "code": "DUPLICATE_VIN",
    "message": "Unit with VIN 1HGCM82633A004352 already exists",
    "details": []
  }
}
```

### Update Unit

```bash
PUT /v1/units/{unitId}
Content-Type: application/json

{
  "year": 2021,
  "make": "Toyota"
}
```

All fields are optional. Only provided fields are updated.

### Delete Unit

```bash
DELETE /v1/units/{unitId}
```

**Response:** 204 No Content

## Data Model

### UnitEntity (DynamoDB)

```java
@Value                                    // Immutable
@Builder                                  // Builder pattern
@With                                     // Wither for safe copying
@AllArgsConstructor                       // Required for DynamoDB schema
public class UnitEntity {
    String unitId;                        // PK
    String customerId;                    // GSI PK
    String vin;                           // GSI PK (unique)
    Integer year;
    String make;
    String model;
    String submodel;
    String engineType;
    String transmissionType;
    String driveType;
    String unitType;
    Map<String, Object> attributes;       // Flexible storage
    Instant createdAt;                    // ISO8601
    Instant updatedAt;                    // ISO8601
}
```

### ID Generation

Unit IDs are generated in format: `unt-{7 random alphanumeric lowercase}`

Example: `unt-a1b2c3d`

Generation uses `SecureRandom` with charset: `abcdefghijklmnopqrstuvwxyz0123456789`

## DynamoDB Schema

### Table: `g-unit-service`

**Billing Mode**: Pay-per-request (on-demand scaling)

**Primary Key**:
- Partition Key: `unitId` (String)

**Global Secondary Indexes**:

1. **vin-index**
   - Partition Key: `vin` (String)
   - Projection: ALL

2. **customerId-index**
   - Partition Key: `customerId` (String)
   - Sort Key: `createdAt` (String - ISO8601)
   - Projection: ALL

## Building & Testing

### Prerequisites

- Java 21+
- Gradle 8.x
- AWS credentials configured (for deployment)

### Build

```bash
./gradlew clean build
```

### Run Tests

```bash
./gradlew test
```

Test Coverage:
- **IdGeneratorTest**: ID format validation, uniqueness
- **UnitMapperTest**: Entity/DTO conversions
- **UnitServiceTest**: Business logic with mocked repository
- **UnitResourceTest**: REST integration (currently disabled)

### Local Development

```bash
# Watch mode with live reload (Lambda dev mode - see note below)
./gradlew quarkusDev

# Build native image
./gradlew build -Dquarkus.native.enabled=true
```

**Note on Dev Mode**: The application runs in AWS Lambda development mode. While the REST endpoints are defined, they're best tested through the unit tests or deployed Lambda. For local HTTP REST testing without Lambda constraints, consider:
- Running the test suite: `./gradlew test`
- Using the integration tests with mocked services
- Deploying to a test Lambda environment

## Configuration

### application.properties

```properties
quarkus.lambda.handler=com.fullbay.unit.LambdaFunctionHandler
quarkus.http.host=localhost
quarkus.http.test-port=0
quarkus.package.type=uber-jar

quarkus.log.level=INFO
quarkus.log.console.json=true

aws.region=us-west-2
dynamodb.table.name=g-unit-service

quarkus.rest.path=/v1
```

### Environment Variables

- `AWS_REGION` - AWS region (default: us-west-2)
- `DYNAMODB_TABLE_NAME` - DynamoDB table name (default: g-unit-service)

## Deployment

### Infrastructure Setup (Terraform)

```bash
cd terraform
terraform init
terraform plan
terraform apply
```

This creates:
- DynamoDB table with GSIs
- Lambda function with SnapStart enabled
- IAM role and policies
- CloudWatch log groups

### Deploy Code

```bash
./gradlew build
aws lambda update-function-code \
  --function-name g-unit-service \
  --zip-file fileb://build/function.zip
```

### Test Deployed Lambda

```bash
aws lambda invoke \
  --function-name g-unit-service \
  --payload '{"httpMethod":"GET","path":"/v1/units"}' \
  response.json

cat response.json
```

## Key Components

### UnitRepository

Data access layer providing:
- `save(entity)` - Insert/update
- `findById(unitId)` - Primary key lookup
- `findByVin(vin)` - GSI query
- `findByCustomerId(customerId)` - GSI query
- `update(entity)` - Update operation
- `delete(unitId)` - Delete operation

All methods wrapped with XRay subsegments for tracing.

### UnitService

Business logic layer:
- **createUnit()** - Generate ID, validate uniqueness, save
- **getUnitById()** - Fetch with not-found handling
- **getUnitByVin()** - Query by VIN
- **getUnitsByCustomerId()** - Query by customer
- **updateUnit()** - Fetch, merge updates, save
- **deleteUnit()** - Verify existence, delete

All methods use XRay tracing and enforce business rules.

### UnitResource

REST endpoints:
- `@GET /units` - List/search
- `@GET /units/{unitId}` - Get one
- `@POST /units` - Create
- `@PUT /units/{unitId}` - Update
- `@DELETE /units/{unitId}` - Delete

All responses wrapped in `ApiResponse<T>` for consistency.

### UnitMapper

Static utility (no instances) providing conversions:
- `toDto(entity)` - Entity → DTO for API responses
- `toEntity(request, unitId)` - Request → Entity for creation
- `updateEntity(entity, request)` - Immutable merge using @Wither

### GlobalExceptionHandler

JAX-RS exception mapper handling:
- `UnitNotFoundException` → 404
- `DuplicateVinException` → 409
- `ConstraintViolationException` → 400 (validation errors)
- Generic `Exception` → 500

All errors returned as `ApiResponse` with `ErrorDetail`.

## Error Handling

### Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | OK | GET successful |
| 201 | Created | POST successful |
| 204 | No Content | DELETE successful |
| 400 | Bad Request | Validation error |
| 404 | Not Found | Unit doesn't exist |
| 409 | Conflict | Duplicate VIN |
| 500 | Internal Error | Unexpected exception |

### Error Response Format

```json
{
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable message",
    "details": [
      {
        "field": "fieldName",
        "message": "Field-specific error"
      }
    ]
  }
}
```

## Performance Optimizations

### SnapStart

Lambda SnapStart reduces cold start times by ~70% by caching an initialized JVM state.

Configuration in `terraform/main.tf`:
```hcl
snap_start {
  apply_on = "PublishedVersions"
}
```

### DynamoDB Optimization

- **On-demand billing** - Scales automatically with traffic
- **Global Secondary Indexes** - Fast queries without full table scans
- **Partition key design** - Distributes load across partitions

### Code-level Optimizations

- **Immutable objects** - No defensive copying needed
- **Final fields/variables** - Compiler optimizations
- **Lazy initialization** - DynamoDB client created on first use
- **XRay batching** - Subsegment annotations for better tracing

## Monitoring & Observability

### XRay Integration

All service operations are traced:

```
unit-service-createUnit
  ├── unit-repository-findByVin
  └── unit-repository-save

unit-service-getUnitById
  └── unit-repository-findById
```

Each operation annotated with:
- `unitId` - Unit identifier
- `customerId` - Customer identifier
- `vin` - Vehicle identification
- `error` - Exception details if thrown

### CloudWatch Logs

Structured JSON logging:
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.fullbay.unit.service.UnitService",
  "message": "Created unit: unt-abc1234"
}
```

## Development Notes

### Immutability Pattern

All updates use the immutable pattern with `@Wither`:

```java
// Instead of:
entity.setYear(2021);

// Use:
final UnitEntity updated = entity.withYear(2021);
```

This creates a new immutable copy, preventing accidental mutations.

### Testing

Tests use Mockito for mocking repository operations:

```java
@ExtendWith(MockitoExtension.class)
class UnitServiceTest {
    @Mock UnitRepository repository;
    private UnitService service;

    @BeforeEach
    void setUp() {
        service = new UnitService(repository);
    }
}
```

No `@QuarkusTest` needed - pure unit testing without CDI overhead.

## Troubleshooting

### Build Issues

**Gradle cache issues:**
```bash
./gradlew clean build --no-build-cache
```

**Lombok annotation processor:**
Check `lombok.config` for proper configuration. Ensure `build.gradle` includes:
```gradle
annotationProcessor 'org.projectlombok:lombok'
```

### Runtime Issues

**DynamoDB connection:**
- Verify AWS credentials: `aws sts get-caller-identity`
- Check region: `echo $AWS_REGION`
- Verify table exists: `aws dynamodb describe-table --table-name g-unit-service`

**Lambda invocation:**
```bash
aws lambda invoke --function-name g-unit-service --payload '{}' response.json
```

## Contributing

- Follow immutability principle: **No Exceptions!**
- Mark all non-reassigned local variables as `final`
- Use builders and withers for object creation/updates
- Add XRay annotations for new operations
- Include unit tests for new features
- Format code with `./gradlew spotlessApply`

## License

Internal FullBay project - All rights reserved.

