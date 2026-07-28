terraform {
  required_version = ">= 1.0.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "eu-north-1"
}


locals {
  bucket_name = "veridoc-pdf-tarolo-2026"
  bucket_arn  = "arn:aws:s3:::veridoc-pdf-tarolo-2026"
}

output "bucket_name" {
  value = local.bucket_name
}

output "bucket_arn" {
  value = local.bucket_arn
}