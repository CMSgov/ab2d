package gov.cms.ab2d.filter;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Cleans out data from a copy of an ExplanationOfBenefit object that we don't want
 * to forward to Part D providers
 */
public class ExplanationOfBenefitsTrimmer {

    /**
     * Pass in an ExplanationOfBenefit, return the copy without the data
     *
     * @param benefit - the original ExplanationOfBenefit
     * @return the cleaned up copy
     */
    public static ExplanationOfBenefit getBenefit(ExplanationOfBenefit benefit) {
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
              patient;
              provider;
              organization;
              facility;
              careTeam
              diagnosis;
              procedure;
              item - Clear out required data

           Inherited - Identifier, resourceType, type
         */
        benefit.setStatus(null);
        benefit.setExtension(null);
        benefit.setPatientTarget(null);
        benefit.setBillablePeriod(null);
        benefit.setCreated(null);
        benefit.setEnterer(null);
        benefit.setEntererTarget(null);
        benefit.setInsurer(null);
        benefit.setInsurerTarget(null);
        benefit.setProviderTarget(null);
        benefit.setOrganizationTarget(null);
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
        clearOutList(benefit.getInformation());
        benefit.setPrecedence(0);
        benefit.setInsurance(null);
        benefit.setAccident(null);
        benefit.setEmploymentImpacted(null);
        benefit.setHospitalization(null);
        if (benefit.getItem() != null) {
            benefit.setItem(benefit.getItem().stream()
                    .map(ExplanationOfBenefitsTrimmer::cleanOutItemComponent)
                    .collect(Collectors.toList()));
        }
        clearOutList(benefit.getAddItem());
        benefit.setTotalCost(null);
        benefit.setUnallocDeductable(null);
        benefit.setTotalBenefit(null);
        benefit.setPayment(null);
        benefit.setForm(null);
        benefit.setContained(null);
        clearOutList(benefit.getProcessNote());
        clearOutList(benefit.getBenefitBalance());
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
              careTeamLinkId
              service
              serviced
              location
              quantity
         */
        clearOutList(component.getDiagnosisLinkId());
        clearOutList(component.getProcedureLinkId());
        clearOutList(component.getInformationLinkId());
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
