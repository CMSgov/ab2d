package gov.cms.ab2d.filter;

import lombok.experimental.UtilityClass;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
 * . created
 * . provider
 * . insurance (focal, coverage)
 * . use (new for R4)
 * . Near Line Record Identification Code (in extension)
 * . CLIA Lab Number
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
 * . detail
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

    public static final String NDC_SYSTEM = "http://hl7.org/fhir/sid/ndc";

    public static final String CLIA_SYSTEM = "http://terminology.hl7.org/NamingSystem/CLIA";
    private static final List<String> roleCodes = List.of("attending", "referring", "operating", "otheroperating", "rendering");

    private static final String RENDERING_EXT = "https://bluebutton.cms.gov/fhir/StructureDefinition/";

    private static final List<String> RENDERING_EXT_URLS = List.of(
            RENDERING_EXT + "CLM-PRVDR-TYPE-CD",
            RENDERING_EXT + "CLM-RNDRG-PRVDR-PRTCPTG-CD"
    );

    /**
     * ALLOWLIST of the ExplanationOfBenefit top-level element names that AB2D shares with
     * Part D providers
     */
    static final Set<String> KEPT_ELEMENTS = Set.of(
            // inherited (Resource / DomainResource)
            "id", "meta", "implicitRules", "language", "text", "contained",
            // ExplanationOfBenefit
            "identifier", "type", "subType", "status", "use", "outcome",
            "patient", "facility", "provider", "billablePeriod",
            "careTeam", "supportingInfo", "diagnosis", "procedure", "item", "insurance"
    );

    /**
     * The set of top-level element names that the in-place trimmer must REMOVE, computed once from
     * the allowlist
     */
    static final List<String> REMOVABLE_ELEMENTS = computeRemovableElements();

    private static List<String> computeRemovableElements() {
        Set<String> removable = new LinkedHashSet<>();
        // Generic HAPI navigation of every declared element on the resource.
        for (Property property : new ExplanationOfBenefit().children()) {
            String name = property.getName();
            if (!KEPT_ELEMENTS.contains(name)) {
                removable.add(name);
            }
        }
        return List.copyOf(removable);
    }

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
     * Mutates the given ExplanationOfBenefit in-place, removing unauthorized fields and
     * filtering contained/careTeam/supportingInfo/insurance without copying the resource or
     * its sub-objects. The existing HAPI-backed lists are pruned with {@code removeIf} and
     * insurance entries are trimmed field-by-field, so no new collections or component
     * objects are allocated. Functionally equivalent to {@link #getBenefit(IBaseResource)}
     * but with no GC pressure from .copy() calls.
     *
     * @param resource - the original ExplanationOfBenefit (will be mutated directly)
     * @return the same instance, sanitized
     */
    public static IBaseResource getBenefitInPlace(IBaseResource resource) {
        if (resource == null) return null;
        ExplanationOfBenefit benefit = (ExplanationOfBenefit) resource;

        // Build lookup map once -- avoids O(NxM) repeated linear scans of the contained list
        Map<String, Resource> containedById = new HashMap<>(benefit.getContained().size() * 2);
        for (Resource r : benefit.getContained()) {
            if (r.getIdPart() != null) containedById.put(r.getIdPart(), r);
        }


        Set<Resource> containedToKeep = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ExplanationOfBenefit.CareTeamComponent ct : benefit.getCareTeam()) {
            if (!matchesRoleCode(ct) || !ct.hasProvider() || !ct.getProvider().hasReference()) continue;
            String ref = ct.getProvider().getReference();
            String id = ref.startsWith("#") ? ref.substring(1) : ref;
            Resource r = containedById.get(id);
            if (r == null || containedToKeep.contains(r)) continue;
            if (hasNpiIdentifier(r) || hasRenderingExtension(r)) containedToKeep.add(r);
        }

        Set<ExplanationOfBenefit.SupportingInformationComponent> cliaSupportingInfo =
                Collections.newSetFromMap(new IdentityHashMap<>());
        for (ExplanationOfBenefit.SupportingInformationComponent si : benefit.getSupportingInfo()) {
            Observation obs = getCliaObservation(si, containedById);
            if (obs != null) {
                cliaSupportingInfo.add(si);
                containedToKeep.add(obs);
            }
        }

        // Prune the original lists in place -- no new collections allocated
        benefit.getContained().removeIf(r -> !containedToKeep.contains(r));
        benefit.getContained().replaceAll(r -> r instanceof Observation obs ? trimCliaObservation(obs) : r);
        benefit.getCareTeam().removeIf(ct -> !matchesRoleCode(ct));
        benefit.getSupportingInfo().removeIf(si ->
                !(matchesNlRecord(si) || matchesDrg(si) || cliaSupportingInfo.contains(si)));
        trimInsuranceInPlace(benefit);

        for (String element : REMOVABLE_ELEMENTS) {
            clearElement(benefit, element);
        }

        // Mutate items in-place without creating a new list
        if (benefit.getItem() != null) {
            benefit.getItem().forEach(ExplanationOfBenefitTrimmerR4V3::cleanOutItemComponent);
        }

        return benefit;
    }

    private static boolean matchesRoleCode(ExplanationOfBenefit.CareTeamComponent ct) {
        if (ct == null || ct.getRole() == null) return false;
        for (Coding c : ct.getRole().getCoding()) {
            if (c.hasCode() && roleCodes.contains(c.getCode())) return true;
        }
        return false;
    }

    private static boolean hasNpiIdentifier(Resource r) {
        for (Identifier ident : extractIdentifiers(r)) {
            if (NPI_SYSTEM.equals(ident.getSystem())) return true;
        }
        return false;
    }

    private static boolean hasRenderingExtension(Resource r) {
        if (!(r instanceof DomainResource dr)) return false;
        for (Extension ext : dr.getExtension()) {
            if (RENDERING_EXT_URLS.contains(ext.getUrl()) && ext.getValue() instanceof CodeableConcept cc) {
                for (Coding coding : cc.getCoding()) {
                    if (coding.hasCode()) return true;
                }
            }
        }
        return false;
    }

    /**
     * Resolve the contained Observation carrying a CLIA Lab Number that this supportingInfo
     * references, per FHIRPath
     * {@code ExplanationOfBenefit.contained.where(id = %root.supportingInfo.valueReference.reference.substring(1)).performer.identifier.value}
     *
     * @param si            the supportingInfo entry
     * @param containedById contained resources of the EOB keyed by id
     * @return the referenced Observation, or null if the entry does not reference a CLIA lab
     */
    private static Observation getCliaObservation(ExplanationOfBenefit.SupportingInformationComponent si,
                                                  Map<String, Resource> containedById) {
        if (!(si.getValue() instanceof Reference ref) || !ref.hasReference()) return null;
        String reference = ref.getReference();
        if (!reference.startsWith("#")) return null;
        if (!(containedById.get(reference.substring(1)) instanceof Observation obs)) return null;
        for (Reference performer : obs.getPerformer()) {
            if (isCliaIdentifier(performer)) return obs;
        }
        return null;
    }

    private static boolean isCliaIdentifier(Reference performer) {
        return performer.hasIdentifier()
                && CLIA_SYSTEM.equals(performer.getIdentifier().getSystem())
                && performer.getIdentifier().hasValue();
    }

    /**
     * Build a minimal copy of a CLIA lab Observation: id plus the elements FHIR requires
     * (status, code) and the CLIA performer identifier. Everything else, including the
     * lab result value, is dropped.
     */
    private static Observation trimCliaObservation(Observation source) {
        Observation trimmed = new Observation();
        trimmed.setId(source.getIdPart());
        trimmed.setStatus(source.getStatus());
        trimmed.setCode(source.getCode().copy());
        for (Reference performer : source.getPerformer()) {
            if (isCliaIdentifier(performer)) {
                trimmed.addPerformer(new Reference().setIdentifier(new Identifier()
                        .setSystem(CLIA_SYSTEM)
                        .setValue(performer.getIdentifier().getValue())));
            }
        }
        return trimmed;
    }

    /**
     * Trim each insurance entry in place to keep only {@code focal} and {@code coverage},
     * dropping the entry's id and (modifier)extensions without allocating new components.
     */
    private static void trimInsuranceInPlace(ExplanationOfBenefit benefit) {
        benefit.getInsurance().removeIf(Objects::isNull);
        for (ExplanationOfBenefit.InsuranceComponent ins : benefit.getInsurance()) {
            ins.setId(null);
            ins.setExtension(null);
            ins.setModifierExtension(null);
        }
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

        Map<String, Resource> containedById = new HashMap<>();
        for (Resource r : benefit.getContained()) {
            if (r.getIdPart() != null) containedById.put(r.getIdPart(), r);
        }
        Set<Resource> cliaContained = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ExplanationOfBenefit.SupportingInformationComponent> cliaSupportingInfo = new ArrayList<>();
        for (ExplanationOfBenefit.SupportingInformationComponent si : benefit.getSupportingInfo()) {
            Observation obs = getCliaObservation(si, containedById);
            if (obs != null) {
                cliaSupportingInfo.add(si);
                cliaContained.add(obs);
            }
        }
        // Keep contained ordering so the output matches the in-place trimmer
        List<Observation> cliaObservations = benefit.getContained().stream()
                .filter(cliaContained::contains)
                .map(r -> trimCliaObservation((Observation) r))
                .toList();

        List<ExplanationOfBenefit.SupportingInformationComponent> supportingInfo = new ArrayList<>();
        // Keep the original ordering so the output matches the in-place trimmer
        for (ExplanationOfBenefit.SupportingInformationComponent si : benefit.getSupportingInfo()) {
            if (matchesNlRecord(si) || matchesDrg(si) || cliaSupportingInfo.contains(si)) {
                supportingInfo.add(si);
            }
        }
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
        contained.addAll(cliaObservations);

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
        copy.setInsurance(copyInsuranceWithFocalAndCoverage(benefit));

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

    private static boolean matchesNlRecord(ExplanationOfBenefit.SupportingInformationComponent si) {
        CodeableConcept code = si.getCode();
        if (code == null) return false;
        for (Coding c : code.getCoding()) {
            if (NL_RECORD_IDENTIFICATION.equalsIgnoreCase(c.getSystem())) return true;
        }
        return false;
    }

    private static boolean matchesDrg(ExplanationOfBenefit.SupportingInformationComponent si) {
        CodeableConcept category = si.getCategory();
        if (category == null || !category.hasCoding()) return false;
        boolean isDrgCategory = false;
        for (Coding c : category.getCoding()) {
            if (c != null && C4BB_SUPPORTING_INFO_TYPE_SYSTEM.equals(c.getSystem())
                    && "drg".equals(c.getCode())) {
                isDrgCategory = true;
                break;
            }
        }
        if (!isDrgCategory) return false;
        CodeableConcept codeConcept = si.getCode();
        if (codeConcept == null || !codeConcept.hasCoding()) return false;
        for (Coding c : codeConcept.getCoding()) {
            if (c != null && MS_DRG_SYSTEM.equals(c.getSystem()) && c.hasCode()) return true;
        }
        return false;
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
            ExplanationOfBenefit benefit,
            List<String> roleCodes
    ) {
        List<ExplanationOfBenefit.CareTeamComponent> result = new ArrayList<>();
        if (benefit == null || benefit.getCareTeam() == null || roleCodes == null || roleCodes.isEmpty()) {
            return result;
        }

        for (ExplanationOfBenefit.CareTeamComponent ct : benefit.getCareTeam()) {
            if (ct == null || ct.getRole() == null) {
                continue;
            }

            boolean matchesRole = false;
            for (Coding c : ct.getRole().getCoding()) {
                if (c.hasCode() && roleCodes.contains(c.getCode())) { matchesRole = true; break; }
            }

            if (matchesRole) {
                result.add(ct);
            }
        }
        return result;
    }

    public static Optional<Resource> getProviderContainedForCareTeam(
            ExplanationOfBenefit benefit,
            ExplanationOfBenefit.CareTeamComponent careTeam
    ) {
        if (benefit == null || careTeam == null || !careTeam.hasProvider() || !careTeam.getProvider().hasReference()) {
            return Optional.empty();
        }

        // "#careteam-provider-3" -> "careteam-provider-3"
        String ref = careTeam.getProvider().getReference();
        String containedId = (ref != null && ref.startsWith("#")) ? ref.substring(1) : ref;
        if (containedId == null) {
            return Optional.empty();
        }

        // Find contained resource with that id
        return benefit.getContained().stream()
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

    public static Optional<Resource> getProviderContainedResource(ExplanationOfBenefit benefit) {
        if (benefit == null || !benefit.hasProvider() || !benefit.getProvider().hasReference()) {
            return Optional.empty();
        }

        String ref = benefit.getProvider().getReference();
        String containedId = ref.startsWith("#") ? ref.substring(1) : ref;
        return benefit.getContained().stream()
                .filter(r -> containedId.equals(r.getIdPart()))
                .findFirst();
    }

    private static List<ExplanationOfBenefit.InsuranceComponent> copyInsuranceWithFocalAndCoverage(ExplanationOfBenefit benefit) {
        List<ExplanationOfBenefit.InsuranceComponent> result = new ArrayList<>();

        List<ExplanationOfBenefit.InsuranceComponent> srcList = benefit.getInsurance();
        for (ExplanationOfBenefit.InsuranceComponent src : srcList) {
            if (src == null) {
                continue;
            }

            ExplanationOfBenefit.InsuranceComponent dst = new ExplanationOfBenefit.InsuranceComponent();
            dst.setCoverage(src.getCoverage().copy());
            dst.setFocal(src.getFocal());
            result.add(dst);
        }

        return result;
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
              detail (National Drug Code + quantity only)
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
        trimDetails(component.getDetail());
        clearOutList(component.getModifierExtension());

        return component;
    }

    /**
     * Keep only item details that carry a National Drug Code, per FHIRPath
     * {@code ExplanationOfBenefit.item.detail.productOrService.where(coding.system='http://hl7.org/fhir/sid/ndc').coding.code},
     * and trim each to sequence, the NDC codings of productOrService, and quantity.
     *
     * @param details - the item's detail list, mutated in place
     */
    private static void trimDetails(List<ExplanationOfBenefit.DetailComponent> details) {
        if (details == null) {
            return;
        }
        details.removeIf(d -> !hasNdc(d.getProductOrService()));
        for (ExplanationOfBenefit.DetailComponent detail : details) {
            CodeableConcept productOrService = detail.getProductOrService();
            productOrService.getCoding().removeIf(c -> !NDC_SYSTEM.equals(c.getSystem()));
            productOrService.setText(null);
            productOrService.setId(null);
            productOrService.setExtension(null);

            detail.setId(null);
            detail.setExtension(null);
            detail.setModifierExtension(null);
            detail.setRevenue(null);
            detail.setCategory(null);
            clearOutList(detail.getModifier());
            clearOutList(detail.getProgramCode());
            detail.setUnitPrice(null);
            detail.setFactor(null);
            detail.setNet(null);
            clearOutList(detail.getUdi());
            clearOutList(detail.getNoteNumber());
            clearOutList(detail.getAdjudication());
            clearOutList(detail.getSubDetail());
        }
    }

    private static boolean hasNdc(CodeableConcept productOrService) {
        if (productOrService == null) return false;
        for (Coding c : productOrService.getCoding()) {
            if (NDC_SYSTEM.equals(c.getSystem()) && c.hasCode()) return true;
        }
        return false;
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

    /**
     * Clear a single top-level element of the EOB in place
     *
     * @param benefit the EOB being trimmed in place
     * @param element the top-level element name to clear
     */
    @SuppressWarnings({"deprecation", "java:S1479"})
    private static void clearElement(ExplanationOfBenefit benefit, String element) {
        switch (element) {
            // inherited
            case "extension" -> benefit.setExtension(null);
            case "modifierExtension" -> benefit.setModifierExtension(null);
            // ExplanationOfBenefit-declared
            case "created" -> benefit.setCreated(null);
            case "enterer" -> benefit.setEnterer(null);
            case "insurer" -> benefit.setInsurer(null);
            case "priority" -> benefit.setPriority(null);
            case "fundsReserveRequested" -> benefit.setFundsReserveRequested(null);
            case "fundsReserve" -> benefit.setFundsReserve(null);
            case "related" -> benefit.setRelated(null);
            case "prescription" -> benefit.setPrescription(null);
            case "originalPrescription" -> benefit.setOriginalPrescription(null);
            case "payee" -> benefit.setPayee(null);
            case "referral" -> benefit.setReferral(null);
            case "claim" -> benefit.setClaim(null);
            case "claimResponse" -> benefit.setClaimResponse(null);
            case "disposition" -> benefit.setDisposition(null);
            case "preAuthRef" -> benefit.setPreAuthRef(null);
            case "preAuthRefPeriod" -> benefit.setPreAuthRefPeriod(null);
            case "precedence" -> benefit.setPrecedenceElement(null);
            case "accident" -> benefit.setAccident(null);
            case "addItem" -> benefit.setAddItem(null);
            case "adjudication" -> benefit.setAdjudication(null);
            case "total" -> benefit.setTotal(null);
            case "payment" -> benefit.setPayment(null);
            case "formCode" -> benefit.setFormCode(null);
            case "form" -> benefit.setForm(null);
            case "processNote" -> benefit.setProcessNote(null);
            case "benefitPeriod" -> benefit.setBenefitPeriod(null);
            case "benefitBalance" -> benefit.setBenefitBalance(null);
            default -> throw new IllegalStateException(
                    "Unhandled removable EOB element '" + element + "'. It is not in the AB2D allowlist "
                            + "(KEPT_ELEMENTS) and has no in-place clear in clearElement(). Decide whether "
                            + "to keep it (add to KEPT_ELEMENTS + copyData) or drop it (add a case here).");
        }
    }
}
