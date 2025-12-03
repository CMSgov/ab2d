package gov.cms.ab2d.filter;

import lombok.experimental.UtilityClass;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Cleans out data from a copy of an ExplanationOfBenefit object that we don't want
 * to forward to Part D providers
 * <p>
 * Keep:
 * . identifier (inherited)
 * . type (inherited, min cardinality from 0 to 1)
 * . meta (inherited)
 * . text (inherited)
 * . language (inherited)
 * . id (inherited)
 * . implicitRules (inherited)
 * . patient
 * . provider
 * . facility
 * . careTeam
 * . diagnosis (new in R4 so don't use - onAdmission)
 * . procedure (new in R4 so don't use - type, udi, udi)
 * . billablePeriod
 * . item (some of the data)
 * . status
 * . Near Line Record Identification Code (in extension)
 * <p>
 * Items not kept:
 * . extension (inherited)
 * except for: Near Line Record Identification Code
 * . modifierExtension (new for R4, inherited)
 * . patientTarget
 * . created
 * . enterer
 * . entererTarget
 * . insurer
 * . insurerTarget
 * . provider
 * . providerTarget
 * . referral
 * . referralTarget
 * . facilityTarget
 * . claim
 * . claimTarget
 * . claimResponse
 * . claimResponseTarget
 * . outcome
 * . disposition
 * . related
 * . prescription
 * . prescriptionTarget
 * . originalPrescription
 * . originalPrescriptionTarget
 * . payee
 * . precedence
 * . insurance
 * . accident
 * . supportingInfo (was information in STU3)
 * . addItem
 * . payment
 * . form
 * . contained
 * . processNote
 * . benefitBalance
 * . priority (new for R4)
 * . total (new for R4)
 * . use (new for R4)
 * . fundsReserveRequested (new for R4)
 * . fundsReserve (new for R4)
 * . preAuthRef (new for R4)
 * . preAuthRefPeriod (new for R4)
 * . formCode (new for R4)
 * . benefitPeriod (new for R4)
 * . adjucation (new for R4)
 * <p>
 * For item elements:
 * <p>
 * Keep:
 * . sequence
 * . careTeamSequence
 * . productOrService
 * . serviced
 * . location
 * . quantity
 * . extension
 * Anesthesia Unit Count
 * Pricing State Code
 * Supplier Type Code
 * <p>
 * Items not kept:
 * . diagnosisSequence
 * . procedureSequence
 * . informationSequence
 * . extension
 * . revenue
 * . category
 * . modifier
 * . programCode
 * . unitPrice
 * . factor
 * . net
 * . udi
 * . bodySite
 * . subSite
 * . encounter
 * . noteNumber
 * . adjudication
 * . detail
 * . modifierExtension
 */
@UtilityClass
public class ExplanationOfBenefitTrimmerR4V3 {
    // 8   public static final String ANESTHESIA_UNIT_COUNT = "https://bluebutton.cms.gov/resources/variables/carr_line_ansthsa_unit_cnt";
    public static final String ANESTHESIA_UNIT_COUNT = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-LINE-ANSTHSA-UNIT-CNT";

    //13   public static final String RELATED_DIAGNOSIS_GROUP = "https://bluebutton.cms.gov/resources/variables/clm_drg_cd";

    //21   public static final String PRICING_STATE = "https://bluebutton.cms.gov/resources/variables/dmerc_line_prcng_state_cd";
    public static final String PRICING_STATE = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRCNG-LCLTY-CD";

    //22   public static final String SUPPLIER_TYPE = "https://bluebutton.cms.gov/resources/variables/dmerc_line_supplr_type_cd";
    public static final String SUPPLIER_TYPE = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRVDR-TYPE-CD";

    //40   public static final String NL_RECORD_IDENTIFICATION = "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd";
    public static final String NL_RECORD_IDENTIFICATION = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-NRLN-RIC-CD";

    public static final String C4BB_SUPPORTING_INFO_TYPE_SYSTEM = "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType";
    public static final String MS_DRG_SYSTEM = "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software";

    private static final String CARETEAM_ROLE_SYSTEM = "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole";

    private static final String NPI_SYSTEM = "http://hl7.org/fhir/sid/us-npi";

    private static final String EXT_PROVIDER_TYPE_URL = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRVDR-TYPE-CD";

    private static final String EXT_RENDERING_PARTICIPATING_URL = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-RNDRG-PRVDR-PRTCPTG-CD";

    /**
     * Pass in an ExplanationOfBenefit, return the copy without the data
     *
     * @param resource - the original ExplanationOfBenefit
     * @return the cleaned up copy
     */
    public static IBaseResource getBenefit(IBaseResource resource) {
        ExplanationOfBenefit benefit = (ExplanationOfBenefit) resource;
        if (benefit == null) {
            return null;
        }
        // Copy it so we don't destroy the original
        ExplanationOfBenefit newBenefit = copyData(benefit);
        // Remove the unauthorized data
        cleanOutUnNeededData(newBenefit);
        // Return the sanitized data
        return newBenefit;
    }

    /**
     * Copy data from the old EOB to the new EOB. Had to do it piecemeal because
     * ExplanationOfBenefit.copy() resulted in a stack overvlow
     *
     * @param benefit - the EOB to copy necessary data
     * @return the new object
     */
    private static ExplanationOfBenefit copyData(ExplanationOfBenefit benefit) {
        ExplanationOfBenefit copy = new ExplanationOfBenefit();
        // Inherited data
        copy.setMeta(benefit.getMeta().copy());
        copy.setType(benefit.getType().copy());
        copy.setText(benefit.getText().copy());
        copy.setSubType(benefit.getSubType().copy());

        List<Identifier> newIds = new ArrayList<>();
        benefit.getIdentifier().forEach(c -> newIds.add(c.copy()));
        copy.setIdentifier(newIds);

        copy.setId(benefit.getId());
        copy.setLanguage(benefit.getLanguage());
        copy.setImplicitRules(benefit.getImplicitRules());

//40        List<Extension> extensions = new ArrayList<>(benefit.getExtensionsByUrl(NL_RECORD_IDENTIFICATION));
//        copy.setExtension(extensions);
        copy.setSupportingInfo(getSupportingInfo(benefit.getSupportingInfo(), NL_RECORD_IDENTIFICATION));
        //13       copy.setSupportingInfo(getSupportingInfo(benefit.getSupportingInfo(), RELATED_DIAGNOSIS_GROUP));
        copy.setSupportingInfo(getSupportingInfo(benefit.getSupportingInfo(), C4BB_SUPPORTING_INFO_TYPE_SYSTEM, MS_DRG_SYSTEM, "drg"));

        // Called out data
        copy.setPatient(benefit.getPatient().copy());
        copy.setFacility(benefit.getFacility().copy());

        List<ExplanationOfBenefit.CareTeamComponent> newCars = new ArrayList<>();
        benefit.getCareTeam().forEach(c -> newCars.add(c.copy()));
        copy.setCareTeam(newCars);

        List<ExplanationOfBenefit.DiagnosisComponent> newDiagnosis = new ArrayList<>();
        benefit.getDiagnosis().forEach(c -> newDiagnosis.add(c.copy()));
        copy.setDiagnosis(newDiagnosis);

        List<ExplanationOfBenefit.ItemComponent> newItem = new ArrayList<>();
        benefit.getItem().forEach(c -> newItem.add(c.copy()));
        copy.setItem(newItem);

        List<ExplanationOfBenefit.ProcedureComponent> newProcedure = new ArrayList<>();
        benefit.getProcedure().forEach(c -> newProcedure.add(c.copy()));
        copy.setProcedure(newProcedure);

        copy.setBillablePeriod(benefit.getBillablePeriod().copy());
        copy.setStatus(benefit.getStatus());

        return copy;
    }

    /**
     * Remove the unauthorized content
     *
     * @param benefit - The ExplanationOfBenefit information (the copy)
     */
    private static void cleanOutUnNeededData(ExplanationOfBenefit benefit) {
        // Remove items in Item Component data
        if (benefit.getItem() != null) {
            benefit.setItem(benefit.getItem().stream()
                    .map(ExplanationOfBenefitTrimmerR4V3::cleanOutItemComponent)
                    .collect(Collectors.toList()));
        }
    }

    /**
     * Retrieve specific supporting information in the list based on the specified system
     *
     * @param supportingInfo - the list of supporing info
     * @param system         - the system to look for
     * @return - Supporting information defined by the passed system
     */
    static List<ExplanationOfBenefit.SupportingInformationComponent> getSupportingInfo(
            List<ExplanationOfBenefit.SupportingInformationComponent> supportingInfo, String system) {
        List<ExplanationOfBenefit.SupportingInformationComponent> newSupporingInfo = new ArrayList<>();
        for (ExplanationOfBenefit.SupportingInformationComponent supporintInfoComponent : supportingInfo) {
            CodeableConcept codeableConcept = supporintInfoComponent.getCode();
            List<Coding> coding = codeableConcept.getCoding();
            for (Coding code : coding) {
                if (system.equalsIgnoreCase(code.getSystem())) {
                    newSupporingInfo.add(supporintInfoComponent);
                    break;
                }
            }
        }
        return newSupporingInfo;
    }

    static List<ExplanationOfBenefit.SupportingInformationComponent> getSupportingInfo(
            List<ExplanationOfBenefit.SupportingInformationComponent> supportingInfo,
            String categorySystem,
            String codeSystem,
            String code) {

        List<ExplanationOfBenefit.SupportingInformationComponent> result = new ArrayList<>();

        if (supportingInfo == null) {
            return result;
        }

        for (ExplanationOfBenefit.SupportingInformationComponent si : supportingInfo) {
            CodeableConcept category = si.getCategory();
            boolean isDrgCategory = false;

            if (category != null && category.hasCoding()) {
                for (Coding coding : category.getCoding()) {
                    if (coding != null
                            && categorySystem.equals(coding.getSystem())
                            && code.equals(coding.getCode())) {
                        isDrgCategory = true;
                        break;
                    }
                }
            }

            if (isDrgCategory) {
                CodeableConcept codeConcept = si.getCode();
                boolean hasMsDrgCode = false;

                if (codeConcept != null && codeConcept.hasCoding()) {
                    for (Coding coding : codeConcept.getCoding()) {
                        if (coding != null
                                && codeSystem.equals(coding.getSystem())
                                && coding.hasCode()) {
                            hasMsDrgCode = true;
                            break;
                        }
                    }
                }

                // Add only if both category + DRG code match
                if (hasMsDrgCode) {
                    result.add(si);
                }
            }
        }
        return result;
    }

    /**
     * Build a new careTeam list for v3 using contained Practitioner resources:
     * <p>
     * - referring:  contained(id = careTeam.referring.provider ref).identifier[us-npi].value
     * - operating:  same as referring (NPI)
     * - otheroperating: same as referring (NPI)
     * - rendering:
     * * identifier[us-npi].value
     * * extension[CLM-PRVDR-TYPE-CD].coding.code
     * * extension[CLM-RNDRG-PRVDR-PRTCPTG-CD].coding.code
     */
    static List<ExplanationOfBenefit.CareTeamComponent> buildNewCareTeams(ExplanationOfBenefit eob) {
        List<ExplanationOfBenefit.CareTeamComponent> result = new ArrayList<>();

        List<ExplanationOfBenefit.CareTeamComponent> srcCareTeam = eob.getCareTeam();
        int size = srcCareTeam != null ? srcCareTeam.size() : 0;

        for (int i = 0; i < size; i++) {
            ExplanationOfBenefit.CareTeamComponent src = srcCareTeam.get(i);

            String roleCode = getRoleCode(src.getRole());
            String practitionerId = getPractitionerIdFromProviderRef(src.getProvider());
            Practitioner practitioner = findContainedPractitioner(eob, practitionerId);

            Identifier npiIdentifier = findNpiIdentifier(practitioner);
            String providerTypeCode = findExtensionCode(practitioner, EXT_PROVIDER_TYPE_URL);
            String renderingParticipatingCode = findExtensionCode(practitioner, EXT_RENDERING_PARTICIPATING_URL);

            boolean shouldAdd = false;
            ExplanationOfBenefit.CareTeamComponent newComponent = null;

            // referring
            if ("referring".equals(roleCode)) {
                if (npiIdentifier != null) {
                    newComponent = makeBaseCareTeamComponent(src, npiIdentifier);
                    shouldAdd = true;
                }
            }

            // operating / otheroperating
            if (!shouldAdd && "operating".equals(roleCode)) {
                if (npiIdentifier != null) {
                    newComponent = makeBaseCareTeamComponent(src, npiIdentifier);
                    shouldAdd = true;
                }
            }

            if (!shouldAdd && "otheroperating".equals(roleCode)) {
                if (npiIdentifier != null) {
                    newComponent = makeBaseCareTeamComponent(src, npiIdentifier);
                    shouldAdd = true;
                }
            }

            // rendering
            if (!shouldAdd && "rendering".equals(roleCode)) {
                boolean hasAnyRenderingData =
                        npiIdentifier != null ||
                                providerTypeCode != null ||
                                renderingParticipatingCode != null;

                if (hasAnyRenderingData) {
                    newComponent = makeBaseCareTeamComponent(src, npiIdentifier);

                    // Map provider type code as an extension on the careTeam (you can move it elsewhere if needed)
                    if (providerTypeCode != null) {
                        Extension providerTypeExt = new Extension(
                                EXT_PROVIDER_TYPE_URL,
                                new CodeType(providerTypeCode)
                        );
                        newComponent.addExtension(providerTypeExt);
                    }

                    // Map rendering participating code as extension
                    if (renderingParticipatingCode != null) {
                        Extension renderingPartExt = new Extension(
                                EXT_RENDERING_PARTICIPATING_URL,
                                new CodeType(renderingParticipatingCode)
                        );
                        newComponent.addExtension(renderingPartExt);
                    }

                    shouldAdd = true;
                }
            }

            if (shouldAdd && newComponent != null) {
                result.add(newComponent);
            }
        }

        return result;
    }

    /**
     * Extract role code from careTeam.role where system = C4BBClaimCareTeamRole.
     */
    private static String getRoleCode(CodeableConcept role) {
        if (role == null) {
            return null;
        }
        List<Coding> codings = role.getCoding();
        int size = codings != null ? codings.size() : 0;

        String code = null;
        for (int i = 0; i < size; i++) {
            Coding c = codings.get(i);
            boolean correctSystem = CARETEAM_ROLE_SYSTEM.equals(c.getSystem());
            if (correctSystem && c.getCode() != null) {
                code = c.getCode();
                i = size; // stop loop without continue
            }
        }
        return code;
    }

    /**
     * From provider.reference like "#careteam-provider-3" → "careteam-provider-3".
     */
    private static String getPractitionerIdFromProviderRef(Reference providerRef) {
        if (providerRef == null) {
            return null;
        }
        String ref = providerRef.getReference();
        if (ref == null) {
            return null;
        }
        String id = null;
        if (ref.startsWith("#") && ref.length() > 1) {
            id = ref.substring(1);
        }
        return id;
    }

    /**
     * Find contained Practitioner with matching id.
     */
    private static Practitioner findContainedPractitioner(ExplanationOfBenefit eob, String id) {
        if (id == null) {
            return null;
        }
        List<Resource> contained = eob.getContained();
        if (contained == null) {
            return null;
        }

        Practitioner result = null;
        int size = contained.size();
        for (int i = 0; i < size; i++) {
            Resource r = contained.get(i);
            if (r instanceof Practitioner && id.equals(r.getId())) {
                result = (Practitioner) r;
                i = size; // stop
            }
        }
        return result;
    }

    /**
     * Practitioner.identifier.where(system = us-npi).first()
     */
    private static Identifier findNpiIdentifier(Practitioner practitioner) {
        if (practitioner == null) {
            return null;
        }
        List<Identifier> identifiers = practitioner.getIdentifier();
        if (identifiers == null) {
            return null;
        }

        Identifier result = null;
        int size = identifiers.size();
        for (int i = 0; i < size; i++) {
            Identifier id = identifiers.get(i);
            if (NPI_SYSTEM.equals(id.getSystem())) {
                result = id;
                i = size; // stop
            }
        }
        return result;
    }

    /**
     * Practitioner.extension.where(url=...).coding.code
     * (takes the first Coding.code on the value if it's a Coding)
     */
    private static String findExtensionCode(Practitioner practitioner, String url) {
        if (practitioner == null || url == null) {
            return null;
        }
        List<Extension> extensions = practitioner.getExtension();
        if (extensions == null) {
            return null;
        }

        String result = null;
        int size = extensions.size();
        for (int i = 0; i < size; i++) {
            Extension ext = extensions.get(i);
            boolean urlMatches = url.equals(ext.getUrl());
            if (urlMatches && ext.getValue() instanceof Coding) {
                Coding coding = (Coding) ext.getValue();
                result = coding.getCode();
                i = size; // stop
            }
        }
        return result;
    }

    private static ExplanationOfBenefit.CareTeamComponent makeBaseCareTeamComponent(
            ExplanationOfBenefit.CareTeamComponent source,
            Identifier npiIdentifier
    ) {
        ExplanationOfBenefit.CareTeamComponent target = source.copy();

        if (npiIdentifier != null) {
            Reference providerRef = target.getProvider();
            if (providerRef == null) {
                providerRef = new Reference();
                target.setProvider(providerRef);
            }

            // copy the whole Identifier (type, period, assigner, etc.)
            providerRef.setIdentifier(npiIdentifier.copy());
        }

        return target;
    }


    /**
     * Used to clean up the ItemComponent because this object is also contains a subset
     * of the data for this object
     *
     * @param component - the data to clean up
     * @return the cleaned up data
     */
    @SuppressWarnings("deprecation")
    private static ExplanationOfBenefit.ItemComponent cleanOutItemComponent(ExplanationOfBenefit.ItemComponent component) {
        /*
         Keep:
              sequence
              diagnosisSequence
              procedureSequence
              careTeamSequence
              productOrService
              serviced
              location
              quantity
              item extensions :
                    Anesthesia Unit Count
                    Pricing State Code
                    Supplier Type Code
              Related Diagnosis Group Code
         */
        clearOutList(component.getInformationSequence());

        // Get the extensions we want to keep
        component.setExtension(findExtensions(component.getExtension(),
                ANESTHESIA_UNIT_COUNT, PRICING_STATE, SUPPLIER_TYPE));

        component.setRevenue(null);
        component.setCategory(null);
        clearOutList(component.getModifier());
        clearOutList(component.getProgramCode());
        component.setUnitPrice(null);
        component.setFactor(null);
        component.setNet(null);
        clearOutList(component.getUdi());
        component.setBodySite(null);
        clearOutList(component.getSubSite());
        clearOutList(component.getEncounter());
        clearOutList(component.getNoteNumber());
        clearOutList(component.getAdjudication());
        clearOutList(component.getDetail());
        clearOutList(component.getModifierExtension());

        return component;
    }

    /**
     * Find specific extensions with the types of url specified by the passed urls
     *
     * @param extensions - the list of extensions to search
     * @param url        - the urls we are searching for
     * @return the list of matching extensions
     */
    static List<Extension> findExtensions(List<Extension> extensions, String... url) {
        List<Extension> keptExtensions = new ArrayList<>();
        if (extensions == null || extensions.isEmpty()) {
            return keptExtensions;
        }
        for (String urlItem : url) {
            Optional<Extension> extension = extensions.stream().filter(e -> e.getUrl().equalsIgnoreCase(urlItem)).findFirst();
            extension.ifPresent(keptExtensions::add);
        }
        return keptExtensions;
    }

    /**
     * Convenience method to clean out a list. If it is null, keep it null,
     * if it is a list with data, empty it out
     *
     * @param list - the list to clear
     */
    static void clearOutList(List<?> list) {
        if (list != null) {
            list.clear();
        }
    }
}
