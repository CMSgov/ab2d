---
title:  "Understanding AB2D Data"
description: CMS is developing a standards-based API to allow standalone Medicare Part D plan (PDP) sponsors to retrieve Medicare claims data for their enrollees.
landing-page: live
gradient: "blueberry-lime-background"
ctas:

---
<style>
table {
    border: 1px;
}
table th {
   text-align: left;
   padding: 5px;
   border-bottom-width: 1px;
}
table td {
   border: 1px;
   vertical-align: top;
   padding: 5px;
}
</style>
<table cellspacing="0" cellpadding="0" width="1000px">
    <thead>
        <tr>
            <th>AB2D&nbsp;Data&nbsp;Element</th>
            <th>FHIR&nbsp;Data&nbsp;Element</th>
            <th>Definition&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</th>
            <th>
                <a target="_blank" href="http://build.fhir.org/conformance-rules.html#cardinality">Cardinality</a>
            </th>
            <th>
                <a target="_blank" href="http://build.fhir.org/terminologies.html">Terminology&nbsp;Binding</a>
            </th>
            <th>
                <a target="_blank" href="http://build.fhir.org/datatypes.html">Type</a>
            </th>
            <th>Comment</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Enrollee identifier</td>
            <td>Patient.identifier</td>
            <td>
                An CCW identifier for this patient in a form of Patient/&lt;identifier&gt; - Display MBI.
            </td>
            <td>1..1</td>
            <td></td>
            <td>
                <a target="_blank" href="http://build.fhir.org/datatypes.html#Identifier">Identifier</a>
            </td>
            <td>
               <a target="_blank" href="https://hl7.org/fhir/STU3/references.html#Reference">
                   Patients are always assigned specific numerical identifiers. See sample JSON
                   page and refer to https://hl7.org/fhir/STU3/references.html#Reference for information
                   on references.</a>
            </td>
        </tr>
        <tr>
            <td>Claim types</td>
            <td>ExplanationOfBenefit.type<br></td>
            <td>
                Claim type code include:<br>
                10-HHA claim<br>
                20-Non swing bed SNF claim<br>
                30-Swing bed SNF claim<br>
                40-Outpatient claim<br>
                50-Hospice claim<br>
                60-Inpatient claim<br>
            </td>
            <td>1..*</td>
            <td>
                <a target="_blank" href="http://build.fhir.org/valueset-claim-type.html">Claim Type
                Codes (Extensible)</a></td>
            <td>CodeableConcept</td>
            <td>
                <a target="_blank" href="https://www.resdac.org/cms-data/variables/nch-claim-type-code">See
                    the sample JSON claim at the end of the page for an example. The list of NCH Claim Type
                    codes is available here.</a>
            </td>
        </tr>
        <tr>
            <td>Diagnosis</td>
            <td>ExplanationOfBenefit.diagnosis</td>
            <td>List of Diagnosis<br></td>
            <td>1..*</td>
            <td>ICD-10 Disease Codes</td>
            <td>Unbound</td>
            <td>
                This data element contains an array of
                    one or more diagnosis. Each diagnosis contains sequence, code, package code, type, reference as well
                    as extensions (e.g. outpatient-clm-poa-ind-sw1-extension).<br>
            </td>
        </tr>
        <tr>
            <td>Procedures</td>
            <td>ExplanationOfBenefit.procedure</td>
            <td>
                A list of Procedures, which identifies the clinical intervention performed
            </td>
            <td>1..*</td>
            <td>
                <a target="_blank" href="http://build.fhir.org/valueset-icd-10-procedures.html">ICD-10
                Procedure Codes</a>
            </td>
            <td>Unbound</td>
            <td>
                <a target="_blank" href="https://hl7.org/fhir/STU3/datatypes.html#CodeableConcept">This
                    data element contains an array of one or more procedures. Each procedure includes
                    sequence, date, and code (as CodeableConcept).</a>
            </td>
        </tr>
        <tr>
            <td rowspan="2">Services performed</td>
            <td>ExplanationOfBenefit.item[].service</td>
            <td>A list of services performed.</td>
            <td>0..1</td>
            <td><a target="_blank" href="https://www.cms.gov/Medicare/Coding/MedHCPCSGenInfo/index.html">HCPCS</a></td>
            <td>CodeableConcept</td>
            <td>Coding for the services performed.</td>
        </tr>
        <tr>
            <td>ExplanationOfBenefit.item[].quantity</td>
            <td>Count of products or services</td>
            <td>0..1</td>
            <td></td>
            <td>SimpleQuantity</td>
            <td><pre>&quot;quantity&quot;:{
    &quot;value&quot;:1
}</pre></td>
        </tr>
        <tr>
            <td rowspan="2">Dates of service</td>
            <td>ExplanationOfBenefit.item[].serviceDate</td>
            <td>Date of service</td>
            <td>0..1</td>
            <td>Date</td>
            <td nowrap="">FHIR Date representation</td>
            <td>
                A JSON string - a union of xs:dateTime, xs:date, xs:gYearMonth, xs:gYear. A date, date-time
                or partial date (e.g. just year or year + month) as used in human communication. If hours
                and minutes are specified, a time zone SHALL be populated. Seconds must be provided due
                to schema type constraints but may be zero-filled and may be ignored.
                Dates SHALL be valid dates.
            </td>
        </tr>
        <tr>
            <td>ExplanationOfBenefit.item[].servicedPeriod</td>
            <td>Period of service</td>
            <td>0..1</td>
            <td></td>
            <td>Period</td>
            <td>
                A period of service, such as<br>
                <pre>&quot;servicedPeriod&quot;:{
    &quot;start&quot;:&quot;2000-10-01&quot;,
    &quot;end&quot;:&quot;2000-10-01&quot;
}</pre>
            </td>
        </tr>
        <tr>
            <td>&nbsp;</td>
            <td>ExplanationOfBenefit.billablePeriod</td>
            <td>Period of service</td>
            <td>0..1</td>
            <td></td>
            <td>Period</td>
            <td>
                A period of service, such as<br>
                <pre>&quot;servicedPeriod&quot;:{
    &quot;start&quot;:&quot;2000-10-01&quot;,
    &quot;end&quot;:&quot;2000-10-01&quot;
}</pre>
                    </td>
                </tr>
        <tr>
            <td>Place of service</td>
            <td>ExplanationOfBenefit.item[].location*</td>
            <td>Facility where the services were provided</td>
            <td>0..1</td>
            <td></td>
            <td>
                One of Reference, Address or Codeable Concept
            </td>
            <td>
                Location can be specified using one of the 3 possible data elements in FHIR EOB:
                locationCodeableConcept, locationAddress or locationReference.
            </td>
        </tr>
        <tr>
            <td rowspan="5">Provider information</td>
            <td>ExplanationOfBenefit.provider</td>
            <td>
                The provider responsible for the claim, predetermination or preauthorization
            </td>
            <td>0..1</td>
            <td></td>
            <td>
                Reference-Patterns&nbsp;Index:<br>&nbsp;&nbsp;&nbsp;&nbsp;Practitioner<br>&nbsp;&nbsp;&nbsp;&nbsp;PractitionerRole<br>&nbsp;&nbsp;&nbsp;&nbsp;Organization
                <br><br>Reference (Practitioner, PractitionerRole, Organization): Common patterns =
                Participant
            </td>
            <td>
                Typically this field would be 1..1 where
                this party is responsible for the claim but not necessarily professionally responsible
                for the provision of the individual products and services listed below.
            </td>
        </tr>
        <tr>
            <td>ExplanationOfBenefit.organization</td>
            <td>The provider&#39;s organization</td>
            <td>0..1</td>
            <td></td>
            <td>
                A Reference to Organization
            </td>
            <td>The provider&#39;s organization; includes at least an org identifier (an NPI).
            </td>
        </tr>
        <tr>
            <td>ExplanationOfBenefit.facility</td>
            <td>The servicing facility</td>
            <td>0..1</td>
            <td></td>
            <td>A Reference to Location</td>
            <td>The servicing facility; includes at least an org identifier.</td>
        </tr>
        <tr>
            <td>ExplanationOfBenefit.careTeam</td>
            <td>
                The members of the team who provided the products or services
            </td>
            <td>0..*</td>
            <td></td>
            <td></td>
        </tr>
        <tr>
            <td>ExplanationOfBenefit.item[].careTeamLinkId</td>
            <td>A link to a Care Team</td>
            <td>0..1</td>
            <td></td>
            <td>PositiveInt</td>
            <td>Applicable care team members</td>
        </tr>
        <tr>
            <td rowspan="3">Claim processing linking identifiers/codes</td>
            <td>ExplanationOfBenefit.identifier</td>
            <td>
                A unique identifier assigned to this explanation of benefit
            </td>
            <td>1..*</td>
            <td></td>
            <td>Identifier</td>
            <td>
                Allows EOBs to be distinguished and referenced. Includes Claim Group ID, which can be
                used to link claims together
            </td>
        </tr>
        <tr>
            <td>ExplanationOfBenefit.type</td>
            <td>Covered earlier</td>
        </tr>
        <tr>
            <td>ExplanationOfBenefit.item.sequence</td>
            <td>A number to uniquel identify item entries</td>
            <td>1..1</td>
            <td></td>
            <td>PositiveInt</td>
            <td>
                Necessary to provide a mechanism to link to items from within the claim and within
                the adjudication details of the ClaimResponse.
            </td>
        </tr>
        <tr>
            <td>Information to support the _since parameter</td>
            <td>ExplanationOfBenefit.meta.lastUpdated</td>
            <td>Data to indicate the time the data was last updated</td>
            <td>0..1</td>
            <td></td>
            <td>Instant</td>
            <td>To support the _since parameter in the API, ExplanationOfBenefit object now searches
            by and returns the last updated date of the ExplanationOfBenefit</td>
        </tr>
    </tbody>
</table>
