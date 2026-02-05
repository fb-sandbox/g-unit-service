output "dynamodb_table_name" {
  description = "DynamoDB table name"
  value       = aws_dynamodb_table.units.name
}

output "dynamodb_table_arn" {
  description = "DynamoDB table ARN"
  value       = aws_dynamodb_table.units.arn
}

output "lambda_function_name" {
  description = "Lambda function name"
  value       = aws_lambda_function.unit_service.function_name
}

output "lambda_function_arn" {
  description = "Lambda function ARN"
  value       = aws_lambda_function.unit_service.arn
}

output "lambda_role_arn" {
  description = "Lambda IAM role ARN"
  value       = aws_iam_role.lambda_role.arn
}

output "current_region" {
  value = data.aws_region.primary.id
}

output "account_id" {
  value = data.aws_caller_identity.current.account_id
}
