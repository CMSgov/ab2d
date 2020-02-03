---
layout: home
title:  "Claims Data to Part D Sponsors Data Dictionary"
date:   2019-11-02 09:21:12 -0500 
description: CMS is developing a standards-based API to allow standalone Medicare Part D plan (PDP) sponsors to retrieve Medicare claims data for their enrollees.
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"
sections:
  - Data Dictionary
ctas:

---

|  AB2D Data Element |  FHIR Data Element |  Definition |  Cardinality |  Terminology Binding |  Type |  Comment |
|:-------------------|:-------------------|:------------|:-------------|:---------------------|:------|:---------|
| Enrollee identifier | Patient.identifier | An identifier for this patient | 1..1 | | Identifier | Patients are always assigned specific numerical identifiers. See sample JSON at the end of the page and refer to https://hl7.org/fhir/STU3/references.html#Reference for information on references. |
| Claim types | ExplanationOfBenefit.type | The category of claim, e.g. oral, pharmacy, vision, institutional, professional | 1..* | Claim Type Codes (Extensible)| Codeable Concept | Claim types determine the general sets of business rules applied for information requirements and adjudication. The majority of jurisdictions use: oral, pharmacy, vision, professional and institutional, or variants on those terms, as the general styles of claims. The valueset is extensible to accommodate other jurisdictional requirements. |
| Diagnosis | ExplanationOfBenefit.diagnosis | List of Diagnosis 1..* | ICD-10 Disease Codes | https://hl7.org/fhir/STU3/terminologies.html#unbound | This data element contains an array of one or more diagnosis. Each diagnosis contains sequence, code, package code, type, reference as well as extensions (e.g. outpatient-clm-poa-ind-sw1-extension). |
| Procedures | ExplanationOfBenefit.procedure | A list of Procedures, which identifies the clinical intervention performed | 1..* | ICD-10 Procedure Codes | https://hl7.org/fhir/STU3/terminologies.html#unbound | This data element contains an array of one or more procedures. Each procedure includes sequence, date, and code (as CodeableConcept). |
| Services performed | ExplanationOfBenefit.item[].service | A list of services performed. | 0..1 | HCPCS | CodeableConcept | Coding for the services performed. |
| | ExplanationOfBenefit.item[].quantity | Count of products or services | 0..1 | | SimpleQuantity | "quantity":{ "value":1 } |
| Dates of service | ExplanationOfBenefit.item[].serviceDate | Date of the service | 0..1 | Date | FHIR Date representation | A JSON string - a union of xs:dateTime, xs:date, xs:gYearMonth, xs:gYear. A date, date-time or partial date (e.g. just year or year + month) as used in human communication. If hours and minutes are specified, a time zone SHALL be populated. Seconds must be provided due to schema type constraints but may be zero-filled and may be ignored. Dates SHALL be valid dates. |
| | ExplanationOfBenefit.item[].servicedPeriod | Period of service | 0..1 | | Period | A period of service, such as "servicedPeriod":{ "start":"2000-10-01", "end":"2000-10-01" } |
| Place of service | ExplanationOfBenefit.item[].location* | Facility where the services were provided | 0..1 | | One of: Reference, Address, CodeableConcept | Location can be specified using one of the 3 possible data elements in FHIR EOB: locationCodeableConcept, locationAddress or locationReference. |
| Provider information | ExplanationOfBenefit.provider | The provider responsible for the claim, predetermination or preauthorization | 0..1 | | Reference Patterns Index: Practitioner PractitionerRole Organization Reference (Practitioner, PractitionerRole, Organization): Common patterns = Participant| Typically this field would be 1..1 where this party is responsible for the claim but not necessarily professionally responsible for the provision of the individual products and services listed below. |
| | ExplanationOfBenefit.organization | The provider's organization | 0..1 | | A Reference to Organization | The provider's organization; includes at least an org identifier (an NPI). |
| | ExplanationOfBenefit.facility | The servicing facility | 0..1 |  | A Reference to Location | The servicing facility; includes at least an org identifier. |
| | ExplanationOfBenefit.careTeam | The members of the team who provided the products and services | 0..* | | | |
| | ExplanationOfBenefit.item[].careTeamLinkId | A link to a Care Team | 0..1 | | Positive Integer | Applicable care team members |
| Claim processing linking identifiers/codes | ExplanationOfBenefit.identifier | A unique identifier assigned to this explanation of benefit. | 1..* | | Identifier | Allows EOBs to be distinguished and referenced. Includes Claim Group ID, which can be used to link claims together |
| | ExplanationOfBenefit.type | Covered earlier | | | | |
| | ExplanationOfBenefit.item.sequence | A number to uniquely identify item entries. | 1..1 | | positiveInt | Necessary to provide a mechanism to link to items from within the claim and within the adjudication details of the ClaimResponse.

### Sample AB2D File

This example was generated from synthetic claims data. Please expand the source to view sample data elements provided for Part D Plan sponsors.

 <textarea>
 {
   "resourceType": "ExplanationOfBenefit",
   "id": "snf-777777777",
   "identifier": [
     {
       "system": "https://bluebutton.cms.gov/resources/variables/clm_id",
       "value": "777777777"
     },
     {
       "system": "https://bluebutton.cms.gov/resources/identifier/claim-group",
       "value": "900"
     }
   ],
   "type": {
     "coding": [
       {
         "system": "https://bluebutton.cms.gov/resources/variables/nch_clm_type_cd",
         "code": "20",
         "display": "Non swing bed Skilled Nursing Facility (SNF) claim"
       },
       {
         "system": "https://bluebutton.cms.gov/resources/codesystem/eob-type",
         "code": "SNF"
       },
       {
         "system": "http://hl7.org/fhir/ex-claimtype",
         "code": "institutional",
         "display": "Institutional"
       },
       {
         "system": "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd",
         "code": "V",
         "display": "Part A institutional claim record (inpatient [IP], skilled nursing facility [SNF], hospice [HOS], or home health agency [HHA])"
       },
       {
         "system": "https://bluebutton.cms.gov/resources/variables/clm_srvc_clsfctn_type_cd",
         "code": "1"
       }
     ]
   },
   "patient": {
     "reference": "Patient/567834"
   },
   "provider": {
     "identifier": {
       "system": "https://bluebutton.cms.gov/resources/variables/prvdr_num",
       "value": "299999"
     }
   },
   "organization": {
     "identifier": {
       "system": "http://hl7.org/fhir/sid/us-npi",
       "value": "1111111111"
     }
   },
   "facility": {
     "extension": [
       {
         "url": "https://bluebutton.cms.gov/resources/variables/clm_fac_type_cd",
         "valueCoding": {
           "system": "https://bluebutton.cms.gov/resources/variables/clm_fac_type_cd",
           "code": "2",
           "display": "Skilled Nursing Facility (SNF)"
         }
       }
     ],
     "identifier": {
       "system": "http://hl7.org/fhir/sid/us-npi",
       "value": "1111111111"
     }
   },
   "careTeam": [
     {
       "sequence": 2,
       "provider": {
         "identifier": {
           "system": "http://hl7.org/fhir/sid/us-npi",
           "value": "3333333333"
         }
       },
       "role": {
         "coding": [
           {
             "system": "http://hl7.org/fhir/claimcareteamrole",
             "code": "assist",
             "display": "Assisting Provider"
           }
         ]
       }
     },
     {
       "sequence": 3,
       "provider": {
         "identifier": {
           "system": "http://hl7.org/fhir/sid/us-npi",
           "value": "4444444444"
         }
       },
       "role": {
         "coding": [
           {
             "system": "http://hl7.org/fhir/claimcareteamrole",
             "code": "other",
             "display": "Other"
           }
         ]
       }
     },
     {
       "sequence": 4,
       "provider": {
         "identifier": {
           "system": "http://hl7.org/fhir/sid/us-npi",
           "value": "2222222222"
         }
       },
       "role": {
         "coding": [
           {
             "system": "http://hl7.org/fhir/claimcareteamrole",
             "code": "primary",
             "display": "Primary provider"
           }
         ]
       }
     },
     {
       "sequence": 5,
       "provider": {
         "identifier": {
           "system": "http://hl7.org/fhir/sid/us-npi",
           "value": "345345345"
         }
       },
       "role": {
         "coding": [
           {
             "system": "http://hl7.org/fhir/claimcareteamrole",
             "code": "primary",
             "display": "Primary provider"
           }
         ]
       }
     }
   ],
   "diagnosis": [
     {
       "packageCode": {
         "coding": [
           {
             "system": "https://bluebutton.cms.gov/resources/variables/clm_drg_cd",
             "code": "645"
           }
         ]
       }
     },
     {
       "sequence": 2,
       "diagnosisCodeableConcept": {
         "coding": [
           {
             "system": "http://hl7.org/fhir/sid/icd-9-cm",
             "code": "R4444"
           }
         ]
       },
       "type": [
         {
           "coding": [
             {
               "system": "https://bluebutton.cms.gov/resources/codesystem/diagnosis-type",
               "code": "admitting",
               "display": "The diagnosis given as the reason why the patient was admitted to the hospital."
             }
           ]
         }
       ]
     },
     {
       "sequence": 3,
       "diagnosisCodeableConcept": {
         "coding": [
           {
             "system": "http://hl7.org/fhir/sid/icd-9-cm",
             "code": "R5555"
           }
         ]
       },
       "type": [
         {
           "coding": [
             {
               "system": "https://bluebutton.cms.gov/resources/codesystem/diagnosis-type",
               "code": "principal",
               "display": "The single medical diagnosis that is most relevant to the patient's chief complaint or need for treatment."
             }
           ]
         }
       ]
     },
     {
       "sequence": 4,
       "diagnosisCodeableConcept": {
         "coding": [
           {
             "system": "http://hl7.org/fhir/sid/icd-9-cm",
             "code": "R6666"
           }
         ]
       }
     },
     {
       "sequence": 5,
       "diagnosisCodeableConcept": {
         "coding": [
           {
             "system": "http://hl7.org/fhir/sid/icd-9-cm",
             "code": "R2222"
           }
         ]
       },
       "type": [
         {
           "coding": [
             {
               "system": "https://bluebutton.cms.gov/resources/codesystem/diagnosis-type",
               "code": "external-first",
               "display": "The code used to identify the 1st external cause of injury, poisoning, or other adverse effect."
             }
           ]
         }
       ]
     },
     {
       "sequence": 6,
       "diagnosisCodeableConcept": {
         "coding": [
           {
             "system": "http://hl7.org/fhir/sid/icd-9-cm",
             "code": "R3333"
           }
         ]
       },
       "type": [
         {
           "coding": [
             {
               "system": "https://bluebutton.cms.gov/resources/codesystem/diagnosis-type",
               "code": "external",
               "display": "A code used to identify an external cause of injury, poisoning, or other adverse effect."
             }
           ]
         }
       ]
     }
   ],
   "procedure": [
     {
       "sequence": 1,
       "date": "2016-01-16T00:00:00-06:00",
       "procedureCodeableConcept": {
         "coding": [
           {
             "system": "http://hl7.org/fhir/sid/icd-9-cm",
             "code": "0TCCCCC"
           }
         ]
       }
     }
   ],
   "precedence": 0,
   "item": [
     {
       "sequence": 1,
       "careTeamLinkId": [
         5
       ],
       "service": {
         "coding": [
           {
             "system": "https://bluebutton.cms.gov/resources/codesystem/hcpcs",
             "code": "MMM"
           }
         ]
       },
       "servicedPeriod": {
         "start": "2000-10-01",
         "end": "2000-10-01"
       },
       "locationAddress": {
         "state": "FL"
       },
       "quantity": {
         "value": 477
       }
     }
   ]
 }
 </textarea>
 
### Additional Resources
Please visit the links below for more information.
 
- [FHIR/HL7](https://www.hl7.org/fhir/)
- [Bulk FHIR specification](http://build.fhir.org/ig/HL7/VhDir/bulk-data.html)
- [Blue Button API](https://bluebutton.cms.gov/developers/)
- [Blue Button Implementation Guide](https://bluebutton.cms.gov/assets/ig/index.html)
- [Intro to JSON Format](http://json.org/) and [http://ndjson.org/](http://ndjson.org)
- [JSON format viewer/validator (raw text/JSON format converter)](https://jsonlint.com/)
- [Intro to valid FHIR formats](http://hl7.org/fhir/STU3/validation.html)