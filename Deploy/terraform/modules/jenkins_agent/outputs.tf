output "jenkins_agent_sec_group_id" {
  value = aws_security_group.jenkins_agent.id
}

output "jenkins_agent_private_ip" {
  value = aws_instance.jenkins_agent.private_ip
}

output "jenkins_agent_id" {
  value = aws_instance.jenkins_agent.id
}
