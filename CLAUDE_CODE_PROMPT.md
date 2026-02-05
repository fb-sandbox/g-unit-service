# Unit Service - Claude Code Prompt

## Overview

Create a REST microservice for managing Units (vehicles/equipment) using Quarkus on AWS Lambda. This is a POC for exploring DynamoDB patterns.

## Source References

**Template to copy from:**
```
/Users/johnnorton/github/fb-sandbox/idp-template-java-fun-master
```

**Target directory:**
```
/Users/johnnorton/github/fb-sandbox/unit-service
```

Copy the entire template structure, then modify for unit-service.

## Tech Stack

- Java 21
- Quarkus with Lambda
- SnapStart enabled
- Lombok for everything (`@Value`, `@Builder`, `@RequiredArgsConstructor`, etc.)
- AWS SDK v2 DynamoDB Enhanced Client
- XRay tracing (follow template patterns)
- Google Java Format (AOSP style)

## Core Principle

**Be Immutable. No Exceptions!**

All data classes must be immutable:
- Use `@Value` instead of `@Data` for DTOs and entities
- No setters
- All fields `final`
- Use builders for construction (`@Builder`)
- Use `@Wither` for safe copying with field updates
- Collections are defensive copied

### Immutable Update Pattern with @Wither

Instead of mutating objects, use Lombok's `@Wither` annotation to generate safe copy methods:

```java
@Value
@Builder
@Wither  // Generates withXxx() methods for each field
public class UnitEntity {
    String unitId;
    String customerId;
    String vin;
    // ...
}

// Usage: chainable immutable updates
UnitEntity updated = entity
    .withVin("NEW_VIN")
    .withYear(2023)
    .withUpdatedAt(Instant.now());
```

Benefits:
- Zero boilerplate code
- Automatic generation of `withXxx()` methods
- Thread-safe immutable copies
- Chainable fluent API
- Preserves other field values automatically

## Package Structure

Base package: `com.fullbay.unit`

```
com.fullbay.unit
├── LambdaFunctionHandler.java      # Keep minimal, delegate to resource
├── config/
│   └── DynamoDbConfig.java         # Enhanced client setup
├── resource/
│   └── UnitResource.java           # REST endpoints
├── service/
│   └── UnitService.java            # Business logic
├── repository/
│   └── UnitRepository.java         # DynamoDB operations
├── model/
│   ├── entity/
│   │   └── UnitEntity.java         # DynamoDB bean
│   ├── dto/
│   │   ├── UnitDto.java            # API request/response
│   │   ├── CreateUnitRequest.java
│   │   └── UpdateUnitRequest.java
│   └── response/
│       ├── ApiResponse.java        # Generic wrapper
│       └── ErrorDetail.java
├── mapper/
│   └── UnitMapper.java             # Entity <-> DTO conversion
└── exception/
    ├── UnitNotFoundException.java
    ├── DuplicateVinException.java
    └── GlobalExceptionHandler.java
```

## REST API

Base path: `/v1/units`

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/v1/units?customerId={id}` | List units by customer |
| GET | `/v1/units?vin={vin}` | Search by VIN |
| GET | `/v1/units/{unitId}` | Get single unit |
| POST | `/v1/units` | Create unit |
| PUT | `/v1/units/{unitId}` | Update unit |
| DELETE | `/v1/units/{unitId}` | Delete unit |

### Response Format

**Success:**
```json
{
  "data": {
    "unitId": "unt-a1b2c3d",
    "customerId": "cst-x1y2z3a",
    "vin": "1HGCM82633A004352",
    "year": 2020,
    "make": "Honda",
    "model": "Accord",
    ...
  }
}
```

**List:**
```json
{
  "data": {
    "items": [...]
  }
}
```

**Error:**
```json
{
  "data": null,
  "error": {
    "code": "UNIT_NOT_FOUND",
    "message": "Unit with id unt-a1b2c3d not found",
    "details": []
  }
}
```

**Validation Error:**
```json
{
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request",
    "details": [
      { "field": "vin", "message": "VIN must be 17 characters" }
    ]
  }
}
```

## Data Model

### UnitEntity (DynamoDB)

```java
@DynamoDbBean
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitEntity {
    private String unitId;          // PK: unt-{a-z0-9:7}
    private String customerId;      // GSI PK: cst-{a-z0-9:7}
    private String vin;             // GSI PK (unique)
    
    // Vehicle fields (ACES-aligned)
    private Integer year;
    private String make;
    private String model;
    private String submodel;
    private String engineType;
    private String transmissionType;
    private String driveType;       // FWD, RWD, AWD, 4WD
    
    private String unitType;        // TRUCK, TRAILER, EQUIPMENT, etc.
    private Map<String, Object> attributes;  // Flexible fields
    
    private Instant createdAt;
    private Instant updatedAt;
}
```

### ID Generation

Generate IDs in format: `unt-{7 random alphanumeric lowercase}`

Example: `unt-a1b2c3d`

Use `java.security.SecureRandom` with characters: `abcdefghijklmnopqrstuvwxyz0123456789`

Create a utility class `IdGenerator` with method `generateUnitId()`.

## DynamoDB Design

**Table name:** `g-unit-service`

**Primary Key:**
- Partition Key: `unitId` (String)

**GSI: vin-index**
- Partition Key: `vin` (String)
- Projection: ALL

**GSI: customerId-index**
- Partition Key: `customerId` (String)
- Sort Key: `createdAt` (String - ISO8601)
- Projection: ALL

### Enhanced Client Setup

```java
@ApplicationScoped
public class DynamoDbConfig {
    
    @Produces
    @ApplicationScoped
    public DynamoDbEnhancedClient enhancedClient() {
        DynamoDbClient client = DynamoDbClient.builder()
            .region(Region.US_WEST_2)
            .build();
        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(client)
            .build();
    }
    
    @Produces
    @ApplicationScoped
    public DynamoDbTable<UnitEntity> unitTable(DynamoDbEnhancedClient client) {
        return client.table("g-unit-service", TableSchema.fromBean(UnitEntity.class));
    }
}
```

### Repository Pattern

Use `@DynamoDbSecondaryPartitionKey` and `@DynamoDbSecondarySortKey` annotations on the entity for GSI definitions.

## Terraform

Create `/terraform` with:

### main.tf

```hcl
# DynamoDB Table
resource "aws_dynamodb_table" "unit_service" {
  name         = "g-unit-service"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "unitId"

  attribute {
    name = "unitId"
    type = "S"
  }

  attribute {
    name = "vin"
    type = "S"
  }

  attribute {
    name = "customerId"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }

  global_secondary_index {
    name            = "vin-index"
    hash_key        = "vin"
    projection_type = "ALL"
  }

  global_secondary_index {
    name            = "customerId-index"
    hash_key        = "customerId"
    range_key       = "createdAt"
    projection_type = "ALL"
  }

  tags = local.common_tags
}

# IAM Role for Lambda
resource "aws_iam_role" "lambda_role" {
  name = "g-unit-service"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

# DynamoDB Policy
resource "aws_iam_role_policy" "dynamodb_policy" {
  name = "dynamodb-access"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Query",
        "dynamodb:Scan"
      ]
      Resource = [
        aws_dynamodb_table.unit_service.arn,
        "${aws_dynamodb_table.unit_service.arn}/index/*"
      ]
    }]
  })
}

# CloudWatch Logs Policy
resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# XRay Policy
resource "aws_iam_role_policy_attachment" "xray" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

# Lambda Function
resource "aws_lambda_function" "unit_service" {
  function_name = "g-unit-service"
  role          = aws_iam_role.lambda_role.arn
  handler       = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  runtime       = "java21"
  timeout       = 15
  memory_size   = 1024

  filename         = "../build/function.zip"
  source_code_hash = filebase64sha256("../build/function.zip")

  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = {
      JAVA_TOOL_OPTIONS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    }
  }

  tags = local.common_tags
}
```

### variables.tf

```hcl
variable "environment" {
  description = "Environment name"
  type        = string
  default     = "sandbox"
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-west-2"
}
```

### locals.tf

```hcl
locals {
  stack_id = "g-unit-service"
  
  common_tags = {
    env        = var.environment
    repo       = local.stack_id
    repoFolder = "${local.stack_id}/terraform"
    createdBy  = "terraform"
  }
}
```

### provider.tf

```hcl
terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}
```

### outputs.tf

```hcl
output "lambda_function_name" {
  value = aws_lambda_function.unit_service.function_name
}

output "lambda_function_arn" {
  value = aws_lambda_function.unit_service.arn
}

output "dynamodb_table_name" {
  value = aws_dynamodb_table.unit_service.name
}

output "dynamodb_table_arn" {
  value = aws_dynamodb_table.unit_service.arn
}
```

## Configuration

### application.properties

```properties
quarkus.lambda.handler=main
quarkus.package.type=uber-jar

# Logging
quarkus.log.level=INFO
quarkus.log.console.json=true

# AWS Region
aws.region=us-west-2

# DynamoDB table name (can be overridden by env var)
dynamodb.table.name=g-unit-service
```

## Key Implementation Notes

1. **Lombok everywhere** - Use `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j` on all classes

2. **Constructor injection** - Use `@RequiredArgsConstructor` with `final` fields, no `@Inject`

3. **XRay tracing** - Follow template patterns with subsegments for repository calls

4. **SnapStart warming** - Add `@PostConstruct` warmup in services like the template shows

5. **No authentication** - Skip auth for this POC

6. **No pagination** - Return all results for list operations

7. **Validation** - Basic validation on VIN (17 chars), year (4 digits), etc.

8. **Error handling** - Global exception handler mapping to consistent error responses

## Update Sherpa Config

Update `sherpa/sherpa.yml`:

```yaml
build:
  image: "533267115767.dkr.ecr.us-west-2.amazonaws.com/idp-builder/java:4fa61b1"
  cmd: "task build"

config:
  shared:
    xray_enabled: false
    memory: 1024
    lambda_runtime: "java21"
    lambda_handler: "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
    snap_start_enabled: true
    timeout_in_seconds: 15
  dev:
    xray_enabled: true
  qa:
    xray_enabled: true
  staging:
    xray_enabled: true
  prod:
    xray_enabled: true
```

## Update build.gradle

Add DynamoDB Enhanced Client dependency:

```gradle
implementation 'software.amazon.awssdk:dynamodb-enhanced'
implementation 'io.quarkus:quarkus-rest-jackson'
implementation 'io.quarkus:quarkus-hibernate-validator'
```

## Testing

Create basic unit tests for:
- `IdGenerator` - verify format `unt-{7 chars}`
- `UnitMapper` - entity to DTO conversion
- `UnitService` - mock repository, test business logic

Follow the test patterns from the template.

## Quick Deploy Commands

After Terraform creates resources:

```bash
# Build
./gradlew build

# Deploy code update
aws lambda update-function-code \
  --function-name g-unit-service \
  --zip-file fileb://build/function.zip

# Test invoke
aws lambda invoke \
  --function-name g-unit-service \
  --payload '{"httpMethod":"GET","path":"/v1/units/unt-test123"}' \
  response.json
```

## File Rename Checklist

After copying template, rename:
- Package directories: `idptemplatefun` → `unit`
- Class names: update to Unit* naming
- `settings.gradle`: update project name
- `build.gradle`: update group if needed
- All imports and references
