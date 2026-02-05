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

output "target_group_arn" {
  description = "ARN of the ALB target group for Lambda"
  value       = aws_lb_target_group.unit_service.arn
}

output "target_group_name" {
  description = "Name of the ALB target group"
  value       = aws_lb_target_group.unit_service.name
}

output "listener_rule_arn" {
  description = "ARN of the ALB listener rule"
  value       = aws_lb_listener_rule.unit_service.arn
}

output "dns_record_fqdn" {
  description = "Fully qualified domain name for the service"
  value       = aws_route53_record.unit_service.fqdn
}

output "alb_dns_name" {
  description = "DNS name of the external ALB"
  value       = data.aws_lb.external.dns_name
}
