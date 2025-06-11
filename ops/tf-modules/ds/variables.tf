#FIXME: should be using AWS SecretsManager for uname/password management from top-to-bottom
variable "username" {}
variable "password" {}
variable "platform" {}
variable "snapshot" {
  default = null
}
