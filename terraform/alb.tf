# Lambda Target Group for ALB
resource "aws_lb_target_group" "unit_service" {
  name        = "g-unit-service"
  target_type = "lambda"

  health_check {
    enabled             = false
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 3
    interval            = 30
  }

  tags = local.common_tags
}

# Allow ALB to invoke Lambda
resource "aws_lambda_permission" "allow_alb" {
  statement_id  = "AllowExecutionFromALB"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.unit_service.function_name
  principal     = "elasticloadbalancing.amazonaws.com"
  source_arn    = aws_lb_target_group.unit_service.arn
}

# Register Lambda with Target Group
resource "aws_lb_target_group_attachment" "unit_service" {
  target_group_arn = aws_lb_target_group.unit_service.arn
  target_id        = aws_lambda_function.unit_service.arn
  depends_on       = [aws_lambda_permission.allow_alb]
}

# ALB Listener Rule - Route by hostname
resource "aws_lb_listener_rule" "unit_service" {
  listener_arn = var.alb_listener_arn

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.unit_service.arn
  }

  condition {
    host_header {
      values = [var.domain_name]
    }
  }

  depends_on = [aws_lb_target_group_attachment.unit_service]
}
