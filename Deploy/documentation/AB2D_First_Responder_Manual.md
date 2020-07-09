# AB2D First Responder Manual

## Table of Contents

1. [Complete first responder prerequites](#first-responder-prerequites)
   * [Obtain VPN access](#obtain-vpn-access)
   * [Obtain CloudTamer access to all AWS accounts](#obtain-cloudtamer-access-to-all-aws-accounts)
   * [Obtain private keys for all AWS accounts](#obtain-private-keys-for-all-aws-accounts)
   * [Install pgAdmin](#install-pgadmin)
   * [Obtain access to VictorOps](#obtain-access-to-victorops)
   * [Obtain access to Splunk](#obtain-access-to-splunk)
   * [Obtain access to New Relic](#obtain-access-to-new-relic)
   * [Obtain access to Cloud Protection Manager](#obtain-access-to-cloud-protection-manager)
1. [Receive on-call message and respond to the incident](#receive-on-call-message-and-respond-to-the-incident)
   * [Receive and acknowkedge on-call message](#receive-and-acknowkedge-on-call-message)
   * [Examine incident in VictorOps](#examine-incident-in-victorops)
   * [Examine Splunk to look for error messages](#examine-splunk-to-look-for-error-messages)
   * [Examine database if applicable](#examine-database-if-applicable)
   * [Make code changes if applicable](#make-code-changes-if-applicable)
   * [Deploy the latest application from master to production](#deploy-the-latest-application-from-master-to-production)

## First responder prerequites

### Obtain VPN access

> *** TO DO ***: to document

### Obtain CloudTamer access to all AWS accounts

> *** TO DO ***: to document

### Obtain private keys for all AWS accounts

> *** TO DO ***: to document

### Install pgAdmin

> *** TO DO ***: to document

### Obtain access to VictorOps

> *** TO DO ***: to document

### Obtain access to Splunk

> *** TO DO ***: to document

### Obtain access to New Relic

> *** TO DO ***: to document

### Obtain access to Cloud Protection Manager

> *** TO DO ***: to document

## Receive on-call message and respond to the incident

### Receive and acknowkedge on-call message

1. Note that you received an SMS message on mobile phone that looks like this

   *Format:*
   
   ```
   {incident number}: VictorOps - {incident message} (Ack: {acknowledged value}, Res: {resolved value})
   ```

   *Example:*
   
   ```
   1055: VictorOps - application failure (Ack: 82603, Res: 20283)
   ```

1. Text the following on your mobile phone to acknowledge the incident

   *Format:*

   ```
   {acknowledged value}
   ```

   *Example:*

   ```
   82603
   ```

### Examine incident in VictorOps

1. Open Chrome

1. Log on to VictorOps

1. Select the **Timeline** tab

1. Note that the following messages should appear in the timeline in reverse chronological order

   - **FIFTH MESSAGE:** Paging cancelled since on-call person has acknowledged receipt of the message

   ```
   Paging cancelled for fred.smith
   ```

   - **FOURTH MESSAGE:** On-call person has acknowledged receipt of the message

   ```
   Incident #1055 AWS CloudWatch
   Jul. 8 - 3:40 PM
   AWS CloudWatch: VictorOps - application failure
   Policies: AB2D : Standard
   Acknowledged by: lonnie.hanekamp
   ```

   - **THIRD MESSAGE:** Page the on-call person

   ```
   Trying to contact fred.smith for #1055, sending SMS
   ```

   - **SECOND MESSAGE:** Incident Message

   ```
   Incident #1055 AWS CloudWatch
   Jul. 8 - 3:37 PM
   AWS CloudWatch: VictorOps - application failure
   Policies: AB2D : Standard
   ```

   - **FIRST MESSAGE:** CloudWatch or NewRelic Alarm

   ```
   AWS CloudWatch
   Jul. 8 - 3:37 PM
   Critical:VictorOps - application failure
   / failure
   NS:
   Region:
   #1055
   ```

1. Examine the messages for details

### Examine Splunk to look for error messages

> *** TO DO ***

### Examine database if applicable

> *** TO DO ***

### Make code changes if applicable

> *** TO DO ***

### Deploy the latest application from master to production

> *** TO DO ***: to document
