package gov.cms.ab2d.coverage.util;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.repository.RoleRepository;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.coverage.repository.CoverageDeltaTestRepository;
import gov.cms.ab2d.coverage.repository.CoveragePeriodRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

//plan on cleaning up when decoupling contract_id foreign keys in database
@Component
public class CoverageDataSetup extends DataSetup {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchRepository coverageSearchRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    CoverageDeltaTestRepository coverageDeltaTestRepository;

    private final Set<Object> domainObjects = new HashSet<>();

    public void queueForCleanup(Object object) {
        domainObjects.add(object);
    }

    @Override
    public void cleanup() {
        // All of the coverage metadata tests assume that you completely
        // wipe the tables between tests and that the tables started as empty tables.
        // Based on these assumptions it is safe to simply delete everything associated
        // with those tables
        coverageDeltaTestRepository.deleteAll();
        coverageDeltaTestRepository.flush();

        deleteCoverage();

        coverageSearchEventRepo.deleteAll();
        coverageSearchEventRepo.flush();

        coverageSearchRepo.deleteAll();
        coverageSearchRepo.flush();

        coveragePeriodRepo.deleteAll();
        coveragePeriodRepo.flush();

        List<Job> jobsToDelete = domainObjects.stream().filter(object -> object instanceof Job)
                .map(object -> (Job) object).collect(toList());
        jobsToDelete.forEach(job -> {
            job = jobRepository.findByJobUuid(job.getJobUuid());
            jobRepository.delete(job);
            jobRepository.flush();
        });

        List<PdpClient> clientsToDelete = domainObjects.stream().filter(object -> object instanceof PdpClient)
                .map(object -> (PdpClient) object).collect(toList());
        clientsToDelete.forEach(pdpClient -> {
            pdpClient = pdpClientRepository.findByClientId(pdpClient.getClientId());

            if (pdpClient != null) {
                pdpClientRepository.delete(pdpClient);
                pdpClientRepository.flush();
            }
        });

        List<Role> rolesToDelete = domainObjects.stream().filter(object -> object instanceof Role)
                .map(object -> (Role) object).collect(toList());
        rolesToDelete.forEach(role -> {
            Optional<Role> roleOptional = roleRepository.findRoleByName(role.getName());

            if (roleOptional.isPresent()) {
                roleRepository.delete(roleOptional.get());
                roleRepository.flush();
            }
        });

        List<Contract> contractsToDelete = domainObjects.stream().filter(object -> object instanceof Contract)
                .map(object -> (Contract) object).collect(toList());
        contractRepository.deleteAll(contractsToDelete);
        contractRepository.flush();

        domainObjects.clear();
    }


    public CoveragePeriod createCoveragePeriod(Contract contract, int month, int year) {
        CoveragePeriod coveragePeriod = new CoveragePeriod();
        coveragePeriod.setContract(contract);
        coveragePeriod.setMonth(month);
        coveragePeriod.setYear(year);

        return coveragePeriodRepo.saveAndFlush(coveragePeriod);
    }


    public void deleteCoverage() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM coverage")) {
            statement.execute();
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public List<Coverage> findCoverage() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM coverage")) {
            ResultSet rs = statement.executeQuery();

            List<Coverage> memberships = new ArrayList<>();
            while (rs.next()) {
                int coveragePeriod = rs.getInt(1);
                long searchEventId = rs.getInt(2);
                String contract = rs.getString(3);
                int year = rs.getInt(4);
                int month = rs.getInt(5);
                long beneficiaryId = rs.getLong(6);
                String currentMbi = rs.getString(7);
                String historicalMbis = rs.getString(8);

                memberships.add(new Coverage(coveragePeriod, searchEventId, contract, year, month, beneficiaryId, currentMbi, historicalMbis));
            }

            return memberships;
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

    public Contract setupContract(String contractNumber) {
        return setupContract(contractNumber, OffsetDateTime.now());
    }

    public Contract setupContract(String contractNumber, OffsetDateTime attestedOn) {
        Contract contract = new Contract();

        contract.setAttestedOn(attestedOn);
        contract.setContractName("Test Contract " + contractNumber);
        contract.setContractNumber(contractNumber);

        contract =  contractRepository.save(contract);
        queueForCleanup(contract);
        return contract;
    }


}
