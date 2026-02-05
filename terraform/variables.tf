variable "environment" {
  description = "Environment name (sandbox, dev, qa, staging, prod)"
  type        = string
  default     = "sandbox"
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-west-2"
}

variable "lambda_zip_path" {
  description = "Path to Lambda deployment ZIP file"
  type        = string
  default     = "../build/function.zip"
}

variable "alb_listener_arn" {
  description = "ARN of the external ALB HTTPS listener"
  type        = string
}

variable "hosted_zone_id" {
  description = "Route53 hosted zone ID for g.fullbay.com"
  type        = string
}

variable "domain_name" {
  description = "Domain name for the service (unit-svc.g.fullbay.com)"
  type        = string
  default     = "unit-svc.g.fullbay.com"
}
