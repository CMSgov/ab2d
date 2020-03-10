output "jenkins_master_sec_group_id" {
  value = aws_security_group.jenkins_master.id
}

output "jenkins_master_private_ip" {
  value = aws_eip.jenkins_master.private_ip
}

output "jenkins_master_id" {
  value = aws_instance.jenkins_master.id
}
