# RDS MySQL for Production
resource "aws_db_subnet_group" "mysql" {
  name       = "${local.cluster_name}-mysql-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${local.cluster_name}-mysql-subnet-group"
  }
}

resource "aws_security_group" "mysql" {
  name_prefix = "${local.cluster_name}-mysql-"
  vpc_id      = aws_vpc.eks_vpc.id

  ingress {
    description     = "MySQL from EKS nodes"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_nodes.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.cluster_name}-mysql-sg"
  }
}

resource "aws_db_instance" "mysql" {
  identifier = "${local.cluster_name}-mysql"
  
  engine         = "mysql"
  engine_version = "8.0"
  instance_class = "db.t3.micro"
  
  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp2"
  storage_encrypted     = true
  
  db_name  = "exam_platform"
  username = "admindb"
  password = "Vaibhav2003"
  
  vpc_security_group_ids = [aws_security_group.mysql.id]
  db_subnet_group_name   = aws_db_subnet_group.mysql.name
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  skip_final_snapshot = true
  deletion_protection = false
  
  tags = {
    Name = "${local.cluster_name}-mysql"
  }
}

output "mysql_endpoint" {
  description = "MySQL RDS endpoint"
  value       = aws_db_instance.mysql.endpoint
}
