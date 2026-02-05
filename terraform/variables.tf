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
