package gov.cms.ab2d.filter;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Cleans out data from a copy of an ExplanationOfBenefit object that we don't want
 * to forward to Part D providers
 */
public class ExplanationOfBenefitTrimmerR4 {

    /**
     * Pass in an ExplanationOfBenefit, return the copy without the data
     *
     * @param b - the original ExplanationOfBenefit
     * @return the cleaned up copy
     */
    public static IBaseResource getBenefit(IBaseResource b) {
        ExplanationOfBenefit benefit = (ExplanationOfBenefit) b;
        if (benefit == null) {
            return null;
        }
        // Copy it so we don't destroy the original
        ExplanationOfBenefit newBenefit = benefit.copy();
        // Remove the unauthorized data
        cleanOutUnNeededData(newBenefit);
        // Return the sanitized data
        return newBenefit;
    }

    /**
     * Remove the unauthorized content
     *
     * @param benefit - The ExplanationOfBenefit information (the copy)
     */
    private static void cleanOutUnNeededData(ExplanationOfBenefit benefit) {
        /*
           Keep:
              patient
              provider
              facility
              careTeam
              diagnosis
              procedure
              billablePeriod
              item - Clear out required data

           Inherited - Identifier, resourceType, type (min cardinality from 0 to 1)
         */

        // We don't know what ends up here so needs to be removed
        clearOutList(benefit.getExtension());
        // This was Information and now it's SupportingInfo
        clearOutList(benefit.getSupportingInfo());

        benefit.setPatientTarget(null);
        benefit.setCreated(null);
        benefit.setEnterer(null);
        benefit.setEntererTarget(null);
        benefit.setInsurer(null);
        benefit.setInsurerTarget(null);
        benefit.setProvider(null);
        benefit.setProviderTarget(null);
        benefit.setReferral(null);
        benefit.setReferralTarget(null);
        benefit.setFacilityTarget(null);
        benefit.setClaim(null);
        benefit.setClaimTarget(null);
        benefit.setClaimResponse(null);
        benefit.setClaimResponseTarget(null);
        benefit.setOutcome(null);
        benefit.setDisposition(null);
        clearOutList(benefit.getRelated());
        benefit.setPrescription(null);
        benefit.setPrescriptionTarget(null);
        benefit.setOriginalPrescription(null);
        benefit.setOriginalPrescriptionTarget(null);
        benefit.setPayee(null);
        benefit.setPrecedence(0);
        clearOutList(benefit.getInsurance());
        benefit.setAccident(null);
        if (benefit.getItem() != null) {
            benefit.setItem(benefit.getItem().stream()
                    .map(ExplanationOfBenefitTrimmerR4::cleanOutItemComponent)
                    .collect(Collectors.toList()));
        }
        clearOutList(benefit.getAddItem());
        benefit.setPayment(null);
        benefit.setForm(null);
        clearOutList(benefit.getContained());
        clearOutList(benefit.getProcessNote());
        clearOutList(benefit.getBenefitBalance());

        // New items for R4 - by default, remove them
        benefit.setPriority(null);
        clearOutList(benefit.getTotal());
        benefit.setUse(null);
        benefit.setFundsReserveRequested(null);
        benefit.setFundsReserve(null);
        clearOutList(benefit.getPreAuthRef());
        clearOutList(benefit.getPreAuthRefPeriod());
        benefit.setFormCode(null);

        if (!benefit.getDiagnosis().isEmpty()) {
            benefit.getDiagnosis().forEach(d -> d.setOnAdmission(null));
        }
        if (!benefit.getProcedure().isEmpty()) {
            benefit.getProcedure().forEach(d -> {
                clearOutList(d.getType());
                clearOutList(d.getUdi());
                clearOutList(d.getUdiTarget());
            });
        }
        benefit.setBenefitPeriod(null);
        clearOutList(benefit.getAdjudication());
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
              careTeamSequence
              productOrService
              serviced
              location
              quantity
         */
        clearOutList(component.getDiagnosisSequence());
        clearOutList(component.getProcedureSequence());
        clearOutList(component.getInformationSequence());
        component.setExtension(null);
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

        // Added in R4

        return component;
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
