# ALB and Route53 Configuration

This Terraform configuration integrates the unit-service Lambda with an external Application Load Balancer (ALB) and creates DNS routing.

## Architecture

```
Client
  ↓
unit-svc.g.fullbay.com (Route53 DNS)
  ↓
ALB (External) - HTTPS Listener
  ↓
ALB Listener Rule (HOST: unit-svc.g.fullbay.com)
  ↓
Target Group (Lambda)
  ↓
Lambda (g-unit-service)
  ↓
DynamoDB
```

## Required Variables

You need to provide these variables via `-var` flags or a `.tfvars` file:

### 1. ALB Listener ARN
The ARN of the external ALB's HTTPS (443) listener that routes traffic.

**Format:**
```
arn:aws:elasticloadbalancing:us-west-2:345594586248:listener/app/{alb-name}/{alb-id}/listener/{listener-id}
```

**How to find it:**
```bash
# List all load balancers
aws elbv2 describe-load-balancers --region us-west-2

# Find the HTTPS listener ARN for the ALB
aws elbv2 describe-listeners \
  --load-balancer-arn arn:aws:elasticloadbalancing:us-west-2:345594586248:loadbalancer/app/g-external-alb/abc123 \
  --region us-west-2
```

**Set via:**
```bash
tofu apply -var='alb_listener_arn=arn:aws:elasticloadbalancing:...'
```

### 2. Route53 Hosted Zone ID
The hosted zone ID for `g.fullbay.com` (starts with `Z`).

**Format:**
```
Z1234567890ABC
```

**How to find it:**
```bash
aws route53 list-hosted-zones-by-name --dns-name g.fullbay.com --region us-west-2
```

**Set via:**
```bash
tofu apply -var='hosted_zone_id=Z1234567890ABC'
```

### 3. Domain Name (Optional)
Default: `unit-svc.g.fullbay.com`

## Deployment

### Option 1: Using Command Line Variables
```bash
cd terraform

AWS_PROFILE=fb-sandbox-non-prod/Admin tofu apply \
  -var='alb_listener_arn=arn:aws:elasticloadbalancing:us-west-2:345594586248:listener/app/...' \
  -var='hosted_zone_id=Z1234567890ABC'
```

### Option 2: Using tfvars File
Create `terraform.tfvars`:
```hcl
alb_listener_arn = "arn:aws:elasticloadbalancing:us-west-2:345594586248:listener/app/g-external-alb/abc123/listener/def456"
hosted_zone_id   = "Z1234567890ABC"
domain_name      = "unit-svc.g.fullbay.com"
```

Then deploy:
```bash
AWS_PROFILE=fb-sandbox-non-prod/Admin tofu apply
```

## Resources Created

1. **ALB Target Group** (`g-unit-service`)
   - Type: Lambda
   - Associated with Lambda function

2. **Lambda Permission**
   - Allows ALB to invoke the Lambda function

3. **ALB Listener Rule**
   - Condition: `HOST = unit-svc.g.fullbay.com`
   - Action: Forward to target group (Lambda)

4. **Route53 DNS Record**
   - Name: `unit-svc.g.fullbay.com`
   - Type: A (Alias)
   - Target: ALB

## Testing

### 1. Verify DNS Resolution
```bash
nslookup unit-svc.g.fullbay.com
```

### 2. Test the Endpoint
```bash
curl -X GET https://unit-svc.g.fullbay.com/v1/units?customerId=cst-test

# With specific VIN
curl -X GET https://unit-svc.g.fullbay.com/v1/units/vin/1HGCM82633A004352

# Create unit from VIN
curl -X POST https://unit-svc.g.fullbay.com/v1/units/vin \
  -H "Content-Type: application/json" \
  -d '{"vin":"1HGCM82633A004352","customerId":"cst-001"}'
```

### 3. Check ALB Target Health
```bash
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancing:us-west-2:345594586248:targetgroup/g-unit-service/abc123 \
  --region us-west-2
```

## Outputs

After deployment, check the outputs:
```bash
tofu output
```

Key outputs:
- `dns_record_fqdn` - The DNS name you should use
- `target_group_arn` - Target group ARN
- `listener_rule_arn` - Listener rule ARN
- `alb_dns_name` - ALB's DNS name

## Troubleshooting

### ALB Health Check Failed
Lambda target groups don't require active health checks. The ALB will forward requests successfully even if health check is disabled.

### DNS Not Resolving
1. Verify Route53 record was created:
   ```bash
   aws route53 list-resource-record-sets \
     --hosted-zone-id Z1234567890ABC | grep unit-svc
   ```

2. Check DNS propagation (can take 5-10 seconds):
   ```bash
   dig @8.8.8.8 unit-svc.g.fullbay.com
   ```

### Lambda Not Invoked
1. Check ALB listener rule exists and is active
2. Verify Lambda permission allows ALB invocation:
   ```bash
   aws lambda get-policy --function-name g-unit-service --region us-west-2
   ```

## Cleanup

To remove ALB and Route53 resources:
```bash
AWS_PROFILE=fb-sandbox-non-prod/Admin tofu destroy -auto-approve
```

Note: This will destroy all resources managed by Terraform, including Lambda, DynamoDB, etc.
