package gov.cms.ab2d.filter;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Cleans out data from a copy of an ExplanationOfBenefit object that we don't want
 * to forward to Part D providers
 *
 *  Keep:
 *     . identifier (inherited)
 *     . type (inherited, min cardinality from 0 to 1)
 *     . meta (inherited)
 *     . text (inherited)
 *     . language (inherited)
 *     . id (inherited)
 *     . implicitRules (inherited)
 *     . patient
 *     . provider
 *     . facility
 *     . careTeam
 *     . diagnosis (new in R4 so don't use - onAdmission)
 *     . procedure (new in R4 so don't use - type, udi, udi)
 *     . billablePeriod
 *     . item (some of the data)
 *     . status
 *     . Near Line Record Identification Code (in extension)
 *
 *  Items not kept:
 *     . extension (inherited)
 *          except for: Near Line Record Identification Code
 *     . modifierExtension (new for R4, inherited)
 *     . patientTarget
 *     . created
 *     . enterer
 *     . entererTarget
 *     . insurer
 *     . insurerTarget
 *     . provider
 *     . providerTarget
 *     . referral
 *     . referralTarget
 *     . facilityTarget
 *     . claim
 *     . claimTarget
 *     . claimResponse
 *     . claimResponseTarget
 *     . outcome
 *     . disposition
 *     . related
 *     . prescription
 *     . prescriptionTarget
 *     . originalPrescription
 *     . originalPrescriptionTarget
 *     . payee
 *     . precedence
 *     . insurance
 *     . accident
 *     . supportingInfo (was information in STU3)
 *     . addItem
 *     . payment
 *     . form
 *     . contained
 *     . processNote
 *     . benefitBalance
 *     . priority (new for R4)
 *     . total (new for R4)
 *     . use (new for R4)
 *     . fundsReserveRequested (new for R4)
 *     . fundsReserve (new for R4)
 *     . preAuthRef (new for R4)
 *     . preAuthRefPeriod (new for R4)
 *     . formCode (new for R4)
 *     . benefitPeriod (new for R4)
 *     . adjucation (new for R4)
 *
 *  For item elements:
 *
 *  Keep:
 *     . sequence
 *     . careTeamSequence
 *     . productOrService
 *     . serviced
 *     . location
 *     . quantity
 *     . extension
 *          Anesthesia Unit Count
 *          Pricing State Code
 *          Supplier Type Code
 *
 *  Items not kept:
 *     . diagnosisSequence
 *     . procedureSequence
 *     . informationSequence
 *     . extension
 *     . revenue
 *     . category
 *     . modifier
 *     . programCode
 *     . unitPrice
 *     . factor
 *     . net
 *     . udi
 *     . udiTarget
 *     . bodySite
 *     . subSite
 *     . encounter
 *     . encounterTarget
 *     . noteNumber
 *     . adjudication
 *     . detail
 *     . modifierExtension
 *
 */
@UtilityClass
public class ExplanationOfBenefitTrimmerR4 {
    public static final String ANESTHESIA_UNIT_COUNT = "https://bluebutton.cms.gov/resources/variables/carr_line_ansthsa_unit_cnt";
    public static final String RELATED_DIAGNOSIS_GROUP = "https://bluebutton.cms.gov/resources/variables/clm_drg_cd";
    public static final String PRICING_STATE = "https://bluebutton.cms.gov/resources/variables/dmerc_line_prcng_state_cd";
    public static final String SUPPLIER_TYPE = "https://bluebutton.cms.gov/resources/variables/dmerc_line_supplr_type_cd";
    public static final String NL_RECORD_IDENTIFICATION = "https://bluebutton.cms.gov/resources/variables/nch_near_line_rec_ident_cd";

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

        List<Extension> extensions = new ArrayList<>(benefit.getExtensionsByUrl(NL_RECORD_IDENTIFICATION));
        copy.setExtension(extensions);

        copy.setSupportingInfo(getSupportingInfo(benefit.getSupportingInfo(), RELATED_DIAGNOSIS_GROUP));

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
                    .map(ExplanationOfBenefitTrimmerR4::cleanOutItemComponent)
                    .collect(Collectors.toList()));
        }
    }

    /**
     * Retrieve specific supporting information in the list based on the specified system
     *
     * @param supportingInfo - the list of supporing info
     * @param system - the system to look for
     *
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
        clearOutList(component.getUdiTarget());
        component.setBodySite(null);
        clearOutList(component.getSubSite());
        clearOutList(component.getEncounter());
        clearOutList(component.getEncounterTarget());
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
     * @param url - the urls we are searching for
     *
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
