---
layout: subpage_layout
title: "Data Dictionary"
date: 2019-11-02 09:21:12 -0500
description: Data Dictionary
landing-page: live
active-nav: understanding-the-data-nav
---

<style>
    .data-table-wrapper {
        background-color: white;
        border: 1px solid #cccccc;
        border-radius: 7px;
        padding: 2px;
    }

    table {
        background-color: #f5f5f5;
    }

    table.data-table {
        display: block;
        overflow-x: auto;
    }

    table thead {
        font-size: 12px;
        text-transform: uppercase;
        color: #748999;
        letter-spacing: 1px;
        background-color: #f7f7f7;
    }

    table tbody {
        font-size: 14px;
    }

    table thead tr:first-child th {
        padding: 20px;
    }

    table thead tr:last-child {
        border-color: #cccccc;
        border-top: 1px solid;
        border-bottom: 1px solid;
    }

    table .top-header :last-child {
        color: #4e5b6b;
        font-style: italic;
        font-size: 13px;
        font-weight: lighter;
        text-align: right;
    }

    table .top-header {
        color: black;
        background-color: white;
        font-size: 18px;
        letter-spacing: 0.5px;

    }

    table .top-header th {
        padding: 20px 5px;
    }

    table tbody tr td,
    table thead tr th {
        padding: 5px 10px;
    }

    table tbody tr td.section-header {
        background-color: #727f8f;
        letter-spacing: 1px;
        color: white;
        text-align: left;
    }

    table tbody tr td.section-header a {
        display: block;
    }

    table tbody tr td.section-header a::before {
        content: '';
        display: block;
        position: relative;
        width: 0;
        height: 5em;
        margin-top: -5em
    }

    .bg-light-grey {
        background-color: #f5f5f5;
    }

    #index {
        margin: auto;
    }

    #index a {
        color: #6c7b8d;
    }

    .sticky {
        position: fixed;
        top: 90px;
        left: 5%;
    }

    #scroll-to-top {
        position: fixed;
        bottom: 20px;
        right: 20px;
        z-index: 2;
        background-color: #323A45;
        padding: 16px;
        border-radius: 50%;
        padding: 13px 17px;
        color: white;
        cursor: pointer;
        display: none;
    }
    
    .show {
        display: block !important;
    }
</style>

<script>
    window.onscroll = function () { scrollSpy() };
    var indexOffset = 0
    $(document).ready(() => {
        offset = $("#index").offset().top - 72
    })

    function scrollSpy() {
        if (window.pageYOffset > offset) {
            $("#index").addClass("sticky");
            $("#scroll-to-top").addClass("show");
        } else {
            $("#index").removeClass("sticky");
            $("#scroll-to-top").removeClass("show");
        }


    } 
</script>

<section class="bg-light-grey page-section py-5" role="main" id="Top">
    <svg class="shape-divider" preserveAspectRatio="xMidYMin slice" version="1.1" xmlns="http://www.w3.org/2000/svg"
        xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" viewBox="0 0 1034.2 43.8"
        style="enable-background:new 0 0 1034.2 43.8;" xml:space="preserve" alt="divider">
        <path fill="#f5f5f5" d="M0,21.3c0,0,209.3-48,517.1,0s517.1,0,517.1,0v22.5H0V21.3z" />
    </svg>
    <a href="#Top" id="scroll-to-top">
        <i class="fas fa-chevron-up"></i>
    </a>
    <div class="container-fluid bg-light-grey">
        <div class="row">
            <div class="col-lg-2">
                <table id="index">
                    <tr>
                        <td><a href="#Patient">Patient</a></td>
                    </tr>
                    <tr>
                        <td><a href="#BillablePeriod">Billable Period</a></td>
                    </tr>
                    <tr>
                        <td><a href="#CareTeam">Care Team</a></td>
                    </tr>
                    <tr>
                        <td><a href="#Claims">Claims</a></td>
                    </tr>
                    <tr>
                        <td><a href="#Diagnosis">Diagnosis</a></td>
                    </tr>
                    <tr>
                        <td><a href="#Item">Item</a></td>
                    </tr>
                    <tr>
                        <td><a href="#Meta">Meta</a></td>
                    </tr>
                    <tr>
                        <td><a href="#Procedure">Procedure</a></td>
                    </tr>
                    <tr>
                        <td><a href="#Provider">Provider</a></td>
                    </tr>
                </table>
            </div>
            <div class="col-lg-10">
                <div class="data-table-wrapper">
                    <table class="data-table">
                        <thead>
                            <tr class="top-header">
                                <th colspan="3">30 Definitions</th>
                                <th colspan="2">Identifiers are grouped by FHIRv3 Data Objects</th>
                            </tr>
                            <tr>
                                <th>Element ID</th>
                                <th>Definition</th>
                                <th>Cardinality</th>
                                <th>Terminology Binding</th>
                                <th>Type</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td class="section-header" colspan="5"><a id="Patient">Patient</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>Patient.Identfier</td>
                                <td>A CCW unique identifier for this patient in a form of Patient &lt;identifier&gt; -
                                    Display
                                    MBI.</td>
                                <td>1..1</td>
                                <td></td>
                                <td></td>
                            </tr>
                            <tr>
                                <td class="section-header" colspan="5"><a id="BillablePeriod">Billable Period</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.billablePeriod</td>
                                <td>The billable period for which charges are being submitted.</td>
                                <td>0..1</td>
                                <td></td>
                                <td><a target="_blank" href="http://hl7.org/fhir/STU3/datatypes.html#Period">Period</a>
                                </td>
                            </tr>
                            <tr>
                                <td class="section-header" colspan="5"><a id="CareTeam">Care Team</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.careTeam</td>
                                <td>The members of the team who provided the overall service as well as their role and
                                    whether responsible and qualifications.</td>
                                <td>0..*</td>
                                <td></td>
                                <td></td>
                            </tr>
                            <tr>
                                <td>ExplanationOfBenefit.careTeam.sequence</td>
                                <td>Sequence of careteam which serves to order and provide a link.</td>
                                <td>1..1</td>
                                <td></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/datatypes.html#positiveInt">positiveInt</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.careTeam.provider</td>
                                <td>The members of the team who provided the overall service.</td>
                                <td>1..1</td>
                                <td></td>
                                <td> <a target="_blank"
                                        href="http://hl7.org/fhir/STU3/references.html#Reference">Reference</a>
                                    (<a target="_blank"
                                        href="http://hl7.org/fhir/STU3/practitioner.html">Practitioner</a> |
                                    <a target="_blank"
                                        href="http://hl7.org/fhir/STU3/organization.html">Organization</a>)
                                </td>
                            </tr>
                            <tr>
                                <td>ExplanationOfBenefit.careTeam.responsible</td>
                                <td>The practitioner who is billing and responsible for the claimed services rendered to
                                    the
                                    patient.</td>
                                <td>0..1</td>
                                <td></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/datatypes.html#boolean">boolean</a>
                                </td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.careTeam.role</td>
                                <td>The lead, assisting or supervising practitioner and their discipline if a
                                    multidisiplinary team.</td>
                                <td>0..1</td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/valueset-claim-careteamrole.html">Claim Care Team
                                        Role Codes (Example)</a></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/datatypes.html#CodeableConcept">CodeableConcept</a>
                                </td>
                            </tr>
                            <tr>
                                <td>ExplanationOfBenefit.careTeam.qualification </td>
                                <td>The qualification which is applicable for this service.</td>
                                <td>0..1</td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/valueset-claim-careteamrole.html">Example
                                        Provider
                                        Qualification Codes (Example)</a></td>
                                <td></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.item.careTeamLinkId</td>
                                <td>Careteam applicable for this service or product line.</td>
                                <td>0..*</td>
                                <td></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/datatypes.html#positiveInt">positiveInt</a></td>
                            </tr>
                            <tr>
                                <td class="section-header" colspan="5"><a id="Claims">Claims</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.identifier</td>
                                <td>The EOB Business Identifier.</td>
                                <td>0..*</td>
                                <td></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/datatypes.html#Identifier">Identifier</a></td>
                            </tr>
                            <tr>
                                <td>ExplanationOfBenefit.type</td>
                                <td>The category of claim, e.g. oral, pharmacy, vision, institutional, professional</td>
                                <td>1..1</td>
                                <td><a target="_blank" href="https://hl7.org/fhir/R4/valueset-claim-type.html">Claim
                                        Type
                                        Codes</a></td>
                                <td><a target="_blank"
                                        href="https://hl7.org/fhir/R4/valueset-claim-type.html">CodeableConcept</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.item.sequence</td>
                                <td>A service line number.</td>
                                <td>1..1</td>
                                <td></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/datatypes.html#positiveInt">positiveInt</a></td>
                            </tr>
                            <tr>
                                <td class="section-header" colspan="5"><a id="Diagnosis">Diagnosis</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.diagnosis</td>
                                <td>Ordered list of patient diagnosis for which care is sought.</td>
                                <td>0..*</td>
                                <td></td>
                                <td></td>
                            </tr>
                            <tr>
                                <td>ExplanationOfBenefit.diagnosis.sequence </td>
                                <td>Sequence of diagnosis which serves to provide a link.</td>
                                <td>1..1</td>
                                <td></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/datatypes.html#positiveInt">positiveInt</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.diagnosis.diagnosis[x]</td>
                                <td>The diagnosis.</td>
                                <td>1..1</td>
                                <td><a target="_blank" href="https://hl7.org/fhir/R4/valueset-icd-10.html">ICD-10
                                        Codes</a>
                                </td>
                                <td><a target="_blank"
                                        href="https://hl7.org/fhir/R4/datatypes.html#CodeableConcept">CodeableConcept |
                                        Reference (Condition)</a></td>
                            </tr>
                            <tr>
                                <td>ExplanationOfBenefit.diagnosis.type</td>
                                <td>The type of the Diagnosis, for example: admitting, primary, secondary, discharge.
                                </td>
                                <td>0..*</td>
                                <td><a target="_blank"
                                        href="https://hl7.org/fhir/R4/valueset-ex-diagnosistype.html">Example
                                        Diagnosis Type Codes</a></td>
                                <td><a target="_blank"
                                        href="https://hl7.org/fhir/R4/valueset-ex-diagnosis-on-admission.html">CodeableConcept</a>
                                </td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.diagnosis.packageCode</td>
                                <td>The package billing code, for example DRG, based on the assigned grouping code
                                    system.
                                </td>
                                <td>0..1</td>
                                <td><a target="_blank"
                                        href="https://hl7.org/fhir/R4/valueset-ex-diagnosisrelatedgroup.html">Example
                                        Diagnosis Related Group Codes</a></td>
                                <td><a target="_blank"
                                        href="https://hl7.org/fhir/R4/valueset-claim-type.html">CodeableConcept</a></td>
                            </tr>
                            <tr>
                                <td class="section-header" colspan="5"><a id="Item">Item</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.item.service</td>
                                <td>If this is an actual service or product line, ie. not a Group, then use code to
                                    indicate
                                    the Professional Service or Product supplied (eg. CTP, HCPCS,USCLS,ICD10,
                                    NCPDP,DIN,ACHI,CCI). If a grouping item then use a group code to indicate the type
                                    of
                                    thing being grouped eg. 'glasses' or 'compound'.</td>
                                <td>0..1</td>
                                <td><a target="_blank" href="http://hl7.org/fhir/STU3/valueset-service-uscls.html">USCLS
                                        Codes</a></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/datatypes.html#CodeableConcept">CodeableConcept</a>
                                </td>
                            </tr>
                            <tr>
                                <td>ExplanationOfBenefit.item.quantity</td>
                                <td>The number of repetitions of a service or product.</td>
                                <td>0..1</td>
                                <td></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/datatypes.html#SimpleQuantity">SimpleQuantity</a>
                                </td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.item[].serviceDate</td>
                                <td>The date or dates when the enclosed suite of services were performed or completed.
                                </td>
                                <td>0..1</td>
                                <td></td>
                                <td></td>
                            </tr>
                            <tr>
                                <td>ExplanationOfBenefit.item[].servicedPeriod</td>
                                <td>The date or dates when the enclosed suite of services were performed or completed.
                                </td>
                                <td>0..1</td>
                                <td></td>
                                <td></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.item.location[x]</td>
                                <td>Where the service was provided.</td>
                                <td>0..1</td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/valueset-service-place.htmlr">Example
                                        Service Place Codes (Example)</a></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/datatypes.html#CodeableConcept">CodeableConcept |
                                        Address | Reference(Location)</a></td>
                            </tr>
                            <tr>
                                <td class="section-header" colspan="5"><a id="Meta">Meta</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.meta.lastUpdated</td>
                                <td>Data to indicate the time the data was last updated</td>
                                <td>0..1</td>
                                <td></td>
                                <td></td>
                            </tr>
                            <tr>
                                <td class="section-header" colspan="5"><a id="Procedure">Procedure</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.procedure</td>
                                <td>Ordered list of patient procedures performed to support the adjudication.</td>
                                <td>0..*</td>
                                <td></td>
                                <td></td>
                            </tr>
                            <tr>
                                <td>ExplanationOfBenefit.procedure.sequence </td>
                                <td>Sequence of procedures which serves to order and provide a link.</td>
                                <td>1..1</td>
                                <td></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/datatypes.html#positiveInt">positiveInt</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.procedure.date</td>
                                <td>Date and optionally time the procedure was performed.</td>
                                <td>0..1</td>
                                <td></td>
                                <td><a target="_blank"
                                        href="https://hl7.org/fhir/R4/datatypes.html#dateTime">dateTime</a>
                                </td>
                            </tr>
                            <tr>
                                <td>ExplanationOfBenefit.procedure.procedure[x]</td>
                                <td>The procedure code.</td>
                                <td>1..1</td>
                                <td><a target="_blank"
                                        href="https://hl7.org/fhir/R4/valueset-icd-10-procedures.html">ICD-10
                                        Procedure Codes</a></td>
                                <td><a target="_blank"
                                        href="https://hl7.org/fhir/R4/datatypes.html#CodeableConcept">CodeableConcept|Reference(Procedure)
                                    </a></td>
                            </tr>
                            <tr>
                                <td class="section-header" colspan="5"><a id="Provider">Provider</a></td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.provider </td>
                                <td>The provider which is responsible for the claim.</td>
                                <td>0..1</td>
                                <td></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/practitioner.html">Reference(Practitioner)</a>
                                </td>
                            </tr>
                            <tr>
                                <td>ExplanationOfBenefit.organization</td>
                                <td>The provider which is responsible for the claim.</td>
                                <td>0..1</td>
                                <td></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/organization.html">Reference(Organization)</a>
                                </td>
                            </tr>
                            <tr class="bg-white">
                                <td>ExplanationOfBenefit.facility</td>
                                <td>Facility where the services were provided.</td>
                                <td>0..1</td>
                                <td></td>
                                <td><a target="_blank"
                                        href="http://hl7.org/fhir/STU3/location.html">Reference(Location)</a>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</section>