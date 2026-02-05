# ALB Configuration
# Replace with your actual ALB HTTPS listener ARN
# Format: arn:aws:elasticloadbalancing:us-west-2:345594586248:listener/app/<alb-name>/<alb-id>/listener/<listener-id>
alb_listener_arn = "arn:aws:elasticloadbalancing:us-west-2:345594586248:listener/app/g-external-alb/a1b2c3d4e5f6g7h8/listener/app-1234567890abcdef"

# Route53 Hosted Zone
# Replace with the hosted zone ID for g.fullbay.com
# Format: Z1234567890ABC
hosted_zone_id = "Z123456789ABC"

# Domain name for this service
domain_name = "unit-svc.g.fullbay.com"

# Environment
environment = "sandbox"

# Lambda ZIP path
lambda_zip_path = "../build/function.zip"
