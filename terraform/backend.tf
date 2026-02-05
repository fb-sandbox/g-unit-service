terraform {
  backend "s3" {
    bucket         = "gauntlet-state-345594586248-us-west-2"
    key            = "g-unit-service/terraform.tfstate"
    region         = "us-west-2"
    encrypt        = true
    dynamodb_table = "terraform-lock"
  }
}
