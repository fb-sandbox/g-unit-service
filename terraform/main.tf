data "aws_caller_identity" "current" {}
data "aws_region" "primary" {}

locals {
  stack_id = "g-unit-service"
  region   = data.aws_region.primary.id

  common_tags = {
    env         = var.environment
    repo        = local.stack_id
    repoFolder  = "${local.stack_id}/terraform"
    createdBy   = "terraform"
  }
}

# DynamoDB Table for Units
resource "aws_dynamodb_table" "units" {
  name           = local.stack_id
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "PK"
  range_key      = "SK"
  stream_enabled = false

  attribute {
    name = "PK"
    type = "S"
  }

  attribute {
    name = "SK"
    type = "S"
  }

  attribute {
    name = "customerId"
    type = "S"
  }

  attribute {
    name = "vin"
    type = "S"
  }

  # GSI for Customer ID + VIN lookups
  global_secondary_index {
    name            = "GSI1-CustomerVin"
    hash_key        = "customerId"
    range_key       = "vin"
    projection_type = "KEYS_ONLY"
  }

  # GSI for VIN-only lookups (across all customers)
  global_secondary_index {
    name            = "GSI2-Vin"
    hash_key        = "vin"
    range_key       = "PK"
    projection_type = "KEYS_ONLY"
  }

  tags = local.common_tags
}

# IAM Role for Lambda
resource "aws_iam_role" "lambda_role" {
  name = local.stack_id

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      },
      {
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::345594586248:root"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = local.common_tags
}

# IAM Policy for DynamoDB access
resource "aws_iam_role_policy" "dynamodb_policy" {
  name = "${local.stack_id}-dynamodb"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:BatchGetItem",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "dynamodb:Query",
          "dynamodb:Scan"
        ]
        Resource = [
          aws_dynamodb_table.units.arn,
          "${aws_dynamodb_table.units.arn}/index/*"
        ]
      }
    ]
  })
}


# IAM Policy for CloudWatch Logs
resource "aws_iam_role_policy_attachment" "lambda_logs_policy" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# IAM Policy for XRay
resource "aws_iam_role_policy_attachment" "lambda_xray_policy" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

# Lambda Function
resource "aws_lambda_function" "unit_service" {
  filename         = var.lambda_zip_path
  function_name   = local.stack_id
  role            = aws_iam_role.lambda_role.arn
  handler         = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  runtime          = "java21"
  timeout          = 15
  memory_size      = 1024
  publish          = true
  source_code_hash = filebase64sha256(var.lambda_zip_path)

  environment {
    variables = {
      DYNAMODB_TABLE_NAME = aws_dynamodb_table.units.name
    }
  }

  snap_start {
    apply_on = "PublishedVersions"
  }

  tags = local.common_tags

  depends_on = [
    aws_iam_role_policy.dynamodb_policy,
    aws_iam_role_policy_attachment.lambda_logs_policy,
    aws_iam_role_policy_attachment.lambda_xray_policy
  ]
}

# Lambda Alias - routes ALB traffic through a published version (required for SnapStart)
resource "aws_lambda_alias" "live" {
  name             = "live"
  function_name    = aws_lambda_function.unit_service.function_name
  function_version = aws_lambda_function.unit_service.version
}
