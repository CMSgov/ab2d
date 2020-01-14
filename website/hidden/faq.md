---
layout: home
title:  "Claims Data to Part D Sponsors"
date:   2019-11-02 09:21:12 -0500 
description: CMS is developing a standards-based API to allow standalone Medicare Part D plan (PDP) sponsors to retrieve Medicare claims data for their enrollees.
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"
sections:
  - Frequently Asked Questions 
ctas:

---

## Frequently Asked Questions
1. **What is the Bipartisan Budget Act of 2018?**<br><br>
In February 2018, the [Bipartisan Budget Act of 2018 (BBA)](https://www.congress.gov/bill/115th-congress/house-bill/1892/text)
was signed into law and it included a provision 
requiring the development of a process to share Medicare claims data with PDP sponsors. Section 50354 of the BBA 
specifically provides that the Secretary of Health and Human Services shall establish a process under which PDP 
sponsors may request, beginning in plan year 2020, that the Secretary provide on a periodic basis and in an electronic 
format standardized extracts of Medicare claims data about its plan enrollees. Such extracts would contain a subset 
of Medicare Parts A and B claims data as determined by the Secretary and would be as current as practicable.<br><br>
2. **What is the Final Rule?**<br><br>
In response to the [Bipartisan Budget Act of 2018 (BBA)](https://www.congress.gov/bill/115th-congress/house-bill/1892/text),
CMS published a 
[Final Rule](https://www.federalregister.gov/documents/2019/04/16/2019-06822/medicare-and-medicaid-programs-policy-and-technical-changes-to-the-medicare-advantage-medicare#page-15745)
 to implement section 50354 of the BBA, which outlines the manner in which CMS proposes to implement this requirement.
<br><br>
3. **Who is eligible to request Medicare Claims data?**<br><br>
Standalone Medicare Part D plan (PDP) sponsors.<br><br>
4. **When can PDP sponsors request data?**<br><br>
The Health Plan Management System (HPMS) Claims Data Attestation module can be used by PDP Sponsors to submit a 
request for Medicare claims data by contract beginning January 1, 2020. The HPMS attestation process ensures PDP 
sponsors are aware of how Medicare claims data may and may not be used, including limitations associated with reuse 
and redisclosure of the data. Visit HPMS.<br><br>
5. **What is Claims Data Attestation?**<br><br>
Attestation performed through the Health Plan Management System (HPMS) Claims Data Attestation module affirms adherence to the permitted uses and limitations for Medicare claims data that listed in § 423.153 of the Final Rule.<br><br>
6. **What is the permitted use of the data?**<br><br>
Section 1860D-4(c)(6)(B) of the Bipartisan Budget Act of 2018 (BBA) specifies that PDP sponsors receiving Medicare Parts A and B claims data for their PDP plan enrollees may use the data for:

    (i) Optimizing therapeutic outcomes through improved medication use;

    (ii) improving care coordination so as to prevent adverse healthcare outcomes, such as preventable emergency 
    department visits and hospital readmissions;

    (iii) for any other purposes determined appropriate by the Secretary.<br>
    
7. **What use of the data is not permitted?**<br><br>
Section 1860D-4(c)(6)(C) of the Bipartisan Budget Act of 2018 (BBA) specifies that PDP sponsors receiving Medicare 
Parts A and B claims data for their PDP plan enrollees may not use the data for the following purposes:

    (i) To inform coverage determinations under this part. 
    
    (ii) To conduct retroactive reviews of medically accepted indications determinations. 
    
    (iii) To facilitate enrollment changes to a different prescription drug plan or an MA-PD plan offered by the 
    same parent organization.
    
    (iv) To inform marketing of benefits.
    
    (v) For any other purpose that the Secretary determines is necessary to include in order to protect the identity 
    of individuals entitled to, or enrolled for, benefits under this title and to protect the security of personal 
    health information.
    
8. **How do PDP sponsors access the data?**<br><br>
PDP sponsors will be able to access the data by leveraging an Application Programming Interface (API).<br>

9. **What is the format of the data extract?**<br><br>
The data will be provided via API in Fast Healthcare Interoperability Resources (FHIR) format.<br><br>
The AB2D API will leverage the Bulk FHIR Specification - http://build.fhir.org/ig/HL7/VhDir/bulk-data.html

10. **What are the data elements that will be accessible through the API?**<br><br>
Medicare Parts A and B claims data elements (fields) in the standardized extract as specified in the rule:
    - An enrollee identifier
    - Diagnosis and procedure codes (for example, ICD-10 diagnosis and Healthcare Common Procedure Coding System (HCPCS) codes)
    - Dates of service
    - Place of service
    - Provider numbers (for example, NPI)
    - Claim processing and linking identifiers/codes (for example, claim ID, and claim type code)
As the API is developed, a full list of the data elements will be shared.<br><br>
11. **Can a PDP sponsor request historical data?**<br><br>
Section 1860D-4(c)(6)(D) of the Act provides that the Secretary shall make standardized extracts available to PDP 
sponsors with data that is the most current as practicable. While we understand that historical data may assist 
PDP sponsors, we must adhere to the statutory language. As this program matures, PDP sponsors will amass historical data.
<br>
12. **What is the lag time for getting claims data?**<br><br>
CMS is working on providing data as current as practicable. The average lag time for a claim being available through 
the API is approximately 2 weeks from the date at which it arrives at CMS. This estimate is based on a number of 
factors and may vary.<br>

13. **How can we get more data elements beside what's listed in the Final Rule?**<br><br>
CMS will continue to evaluate the data elements provided to PDP sponsors to determine if data elements should be added 
or removed based on the information needed to carry out the permitted uses of the data. Any proposed changes would be 
established through rulemaking.<br>

14. **What are the data sources and how often is the data updated?**<br><br>
The AB2D API will leverage the Blue Button FHIR Data server, which gets the data from the Chronic Condition 
Warehouse (CCW). The data is refreshed every weekend.
