---
layout: home
title:  "Understanding AB2D Data"
date:   2019-11-02 09:21:12 -0500 
description: CMS is developing a standards-based API to allow standalone Medicare Part D plan (PDP) sponsors to retrieve Medicare claims data for their enrollees.
landing-page: live
gradient: "blueberry-lime-background"
subnav-link-gradient: "blueberry-lime-link"
sections:
  - Data Use and Limitations 
  - Data Dictionary
  - Sample AB2D File
  - Additional Resources
ctas:

---
<style>
textarea {
    font-family: monospace, monospace;
    font-size: 1em;    
    background-color: #f3f6fa;
    border: solid 1px #dddddd;
    font: 1rem Consolas, "Liberation Mono", Menlo, Courier, monospace;
    font-size: .8rem;
    line-height: 1.25;
    color: #567482;
}
</style>

## Data Use and Limitations

Data will be limited to a subset of explanation of benefit data records by the following constraints:

- Only claim data belonging to the PDP sponsor's active enrollees list
- Only data from active enrollees who did not opt out of data sharing by calling 1-(800)-Medicare (1-800-633-4227)
- Only Part A and Part B data, Part D data is excluded
- Claims with disease codes related to Substance Abuse and Mental Health are excluded
- Only data specified by the Secretary of Health and Human Services within the explanation of benefit object (not all 
data in the explanation of benefit object is included in the returned object)

This data may be used for:

- Optimizing therapeutic outcomes through improved medication use
- Improving care coordination so as to prevent adverse healthcare outcomes, such as preventable emergency department 
visits and hospital readmissions
- For any other purposes determined appropriate by the Secretary

The sponsors may not use the data:

- To inform coverage determination under Part D
- To conduct retroactive reviews of medically accepted conditions
- To facilitate enrollment changes to a different or a MA-PD plan offered b the same parent organization
- To inform marketing of benefits
- For any other purpose the Secretary determines is necessary to include in order to protect the identity of 
individuals entitled to or enrolled in Medicare, and to protect the security of personal health information.

## Data Dictionary

The full data dictionary is [here](data_dictionary).

The description of the data used (and reused) elements is found [here](data_types).

## Sample AB2D File

This example was generated from synthetic claims data. Please expand the source to view sample data elements provided for Part D Plan sponsors.

 <textarea cols="100" rows="50">
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
   "billablePeriod": {
   	 "start": "1999-10-27",
   	 "end": "1999-10-27"
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
 
## Additional Resources
Please visit the links below for more information.
 
- [FHIR/HL7](https://www.hl7.org/fhir/)
- [Bulk FHIR specification](http://build.fhir.org/ig/HL7/VhDir/bulk-data.html)
- [Blue Button API](https://bluebutton.cms.gov/developers/)
- [Blue Button Implementation Guide](https://bluebutton.cms.gov/assets/ig/index.html)
- [Intro to JSON Format](http://json.org/) and [http://ndjson.org/](http://ndjson.org)
- [JSON format viewer/validator (raw text/JSON format converter)](https://jsonlint.com/)
- [Intro to valid FHIR formats](http://hl7.org/fhir/STU3/validation.html)