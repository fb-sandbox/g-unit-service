# Get the ALB data from the listener
data "aws_lb" "external" {
  arn = data.aws_lb_listener.external.load_balancer_arn
}

data "aws_lb_listener" "external" {
  arn = var.alb_listener_arn
}

# Route53 DNS Record - Point to ALB
resource "aws_route53_record" "unit_service" {
  zone_id = var.hosted_zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = data.aws_lb.external.dns_name
    zone_id                = data.aws_lb.external.zone_id
    evaluate_target_health = false
  }
}
