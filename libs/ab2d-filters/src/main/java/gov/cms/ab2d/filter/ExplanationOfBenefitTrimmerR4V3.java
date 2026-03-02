package gov.cms.ab2d.filter;

import lombok.experimental.UtilityClass;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.Collections;
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
 * . enterer
 * . entererTarget
 * . insurer
 * . insurerTarget
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
    public static final String ANESTHESIA_UNIT_COUNT = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-LINE-ANSTHSA-UNIT-CNT";

    public static final String PRICING_STATE = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRCNG-LCLTY-CD";

    public static final String SUPPLIER_TYPE = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRVDR-TYPE-CD";

    public static final String NL_RECORD_IDENTIFICATION = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-NRLN-RIC-CD";

    public static final String C4BB_SUPPORTING_INFO_TYPE_SYSTEM = "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType";
    public static final String MS_DRG_SYSTEM = "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software";

    private static final String NPI_SYSTEM = "http://hl7.org/fhir/sid/us-npi";
    private static final List<String> roleCodes = List.of("attending", "referring", "operating", "otheroperating", "rendering");

    private static final String RENDERING_EXT = "https://bluebutton.cms.gov/fhir/StructureDefinition/";

    private static final List<String> RENDERING_EXT_URLS = List.of(
            RENDERING_EXT + "CLM-PRVDR-TYPE-CD",
            RENDERING_EXT + "CLM-RNDRG-PRVDR-PRTCPTG-CD"
    );

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
        List<Resource> contained = new ArrayList<>();
        copy.setContained(contained);
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

        List<ExplanationOfBenefit.SupportingInformationComponent> supportingInfo = new ArrayList<>();
        supportingInfo.addAll(getSupportingInfo(benefit.getSupportingInfo(), NL_RECORD_IDENTIFICATION));
        supportingInfo.addAll(getSupportingInfo(benefit.getSupportingInfo(), C4BB_SUPPORTING_INFO_TYPE_SYSTEM, MS_DRG_SYSTEM, "drg"));
        copy.setSupportingInfo(supportingInfo);
        // Called out data
        copy.setPatient(benefit.getPatient().copy());
        copy.setFacility(benefit.getFacility().copy());

        if (benefit.hasProvider()) {
            copy.setProvider(benefit.getProvider().copy());

            // Also copy referenced contained Organization if present
            getProviderContainedResource(benefit)
                    .ifPresent(org -> copy.getContained().add(org.copy()));
        }

        List<ExplanationOfBenefit.CareTeamComponent> newCars = getCareTeamsByRoleCodes(benefit, roleCodes);

        copy.setCareTeam(newCars);

        List<Resource> npiContained =
                newCars.stream()
                        .flatMap(ct -> getProviderContainedForCareTeam(benefit, ct).stream())
                        .filter(res -> extractIdentifiers(res).stream()
                                .anyMatch(id -> NPI_SYSTEM.equals(id.getSystem())))
                        .toList();

        List<Resource> withRenderingExtensions =
                newCars.stream()
                        .flatMap(ct -> getProviderContainedForCareTeam(benefit, ct).stream())
                        .filter(DomainResource.class::isInstance)
                        .map(DomainResource.class::cast)
                        .filter(dr -> dr.getExtension().stream()
                                .filter(ext -> RENDERING_EXT_URLS.contains(ext.getUrl()))
                                .anyMatch(ext -> ext.getValue() instanceof CodeableConcept cc &&
                                        cc.getCoding().stream().anyMatch(Coding::hasCode)))
                        .collect(Collectors.toList());
        contained.addAll(npiContained);
        contained.addAll(withRenderingExtensions);

        copy.setContained(contained);


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

        copy.setUse(benefit.getUse());
        copy.setOutcome(benefit.getOutcome());
        copy.setInsurance(copyInsuranceWithFocalAndCoverage(benefit, contained));

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
                    .toList());
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

    public static List<ExplanationOfBenefit.CareTeamComponent> getCareTeamsByRoleCodes(
            ExplanationOfBenefit eob,
            List<String> roleCodes
    ) {
        List<ExplanationOfBenefit.CareTeamComponent> result = new ArrayList<>();
        if (eob == null || eob.getCareTeam() == null || roleCodes == null || roleCodes.isEmpty()) {
            return result;
        }

        for (ExplanationOfBenefit.CareTeamComponent ct : eob.getCareTeam()) {
            if (ct == null || ct.getRole() == null) {
                continue;
            }

            boolean matchesRole = ct.getRole().getCoding().stream()
                    .anyMatch(c -> c.hasCode() && roleCodes.contains(c.getCode()));

            if (matchesRole) {
                result.add(ct);
            }
        }
        return result;
    }

    public static Optional<Resource> getProviderContainedForCareTeam(
            ExplanationOfBenefit eob,
            ExplanationOfBenefit.CareTeamComponent careTeam
    ) {
        if (eob == null || careTeam == null || !careTeam.hasProvider() || !careTeam.getProvider().hasReference()) {
            return Optional.empty();
        }

        // "#careteam-provider-3" -> "careteam-provider-3"
        String ref = careTeam.getProvider().getReference();
        String containedId = (ref != null && ref.startsWith("#")) ? ref.substring(1) : ref;
        if (containedId == null) {
            return Optional.empty();
        }

        // Find contained resource with that id
        return eob.getContained().stream()
                .filter(r -> containedId.equals(r.getIdPart()))
                .findFirst();
    }


    private static List<Identifier> extractIdentifiers(Resource resource) {
        if (resource instanceof Practitioner p) {
            return p.getIdentifier();
        }
        if (resource instanceof Organization o) {
            return o.getIdentifier();
        }
        return Collections.emptyList();
    }

    public static Optional<Resource> getProviderContainedResource(ExplanationOfBenefit eob) {
        if (eob == null || !eob.hasProvider() || !eob.getProvider().hasReference()) {
            return Optional.empty();
        }

        String ref = eob.getProvider().getReference();
        String containedId = ref.startsWith("#") ? ref.substring(1) : ref;
        return eob.getContained().stream()
                .filter(r -> containedId.equals(r.getIdPart()))
                .findFirst();
    }

    private static List<ExplanationOfBenefit.InsuranceComponent> copyInsuranceWithFocalAndCoverage(
            ExplanationOfBenefit source,
            List<Resource> targetContained
    ) {
        List<ExplanationOfBenefit.InsuranceComponent> result = new ArrayList<>();
        if (source == null || !source.hasInsurance()) {
            throw new IllegalStateException("EOB missing insurance; cannot set required insurance.focal and insurance.coverage");
        }

        List<ExplanationOfBenefit.InsuranceComponent> srcList = source.getInsurance();
        boolean focalSet = false;
        for (int i = 0; i < srcList.size(); i++) {
            ExplanationOfBenefit.InsuranceComponent src = srcList.get(i);
            if (src == null) {
                continue;
            }

            ExplanationOfBenefit.InsuranceComponent dst = new ExplanationOfBenefit.InsuranceComponent();
            dst.setCoverage(src.getCoverage().copy());
            boolean srcFocal = src.hasFocal() && src.getFocal();
            if (!focalSet && srcFocal) {
                dst.setFocal(true);
                focalSet = true;
            } else {
                dst.setFocal(false);
            }
            result.add(dst);
            addContainedIfReferenced(source, dst.getCoverage(), targetContained);
        }
        if (!result.isEmpty() && !focalSet) {
            result.get(0).setFocal(true);
        }

        return result;
    }

    private static <T extends Resource> void addContainedIfReferenced(
            ExplanationOfBenefit source,
            Reference ref,
            List<Resource> targetContained
    ) {
        if (source == null || ref == null || !ref.hasReference()) {
            return;
        }
        String reference = ref.getReference();
        if (reference == null || !reference.startsWith("#")) {
            // External reference; nothing to copy into contained
            return;
        }
        String id = reference.substring(1);
        Optional<Resource> match = source.getContained().stream()
                .filter(r -> id.equals(r.getIdPart()))
                .findFirst();
        if (match.isEmpty()) {
            return;
        }
        Resource res = match.get();
        // Ensure type matches
        if (!(res instanceof Coverage)) {
            return; // or throw if you want strictness
        }
        // Avoid duplicates in target contained
        boolean alreadyPresent = targetContained.stream().anyMatch(r -> id.equals(r.getIdPart()));
        if (!alreadyPresent) {
            targetContained.add(res.copy());
        }
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
        return ExplanationOfBenefitTrimmerR4.findExtensions(extensions, url);
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
