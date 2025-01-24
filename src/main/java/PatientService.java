import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PatientService {
    public static final String FILE_NAME = "family-names.txt";
    public static final int MAX_NUM_LOOPS = 3;

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(PatientService.class);

    /**
     * Task 1:
     * Print the patient names sorted by given name
     */
    public void printSortedPatientNames(List<Bundle.BundleEntryComponent> entries) {
        if (CollectionUtils.isEmpty(entries)) {
            LOG.info("The bundle entry list has no elements");
            return;
        }
        entries.sort((e1, e2) -> {
            Patient p1 = (Patient) e1.getResource();
            Patient p2 = (Patient) e2.getResource();
            return p1.getName().get(0).getGivenAsSingleString().compareTo(p2.getName().get(0).getGivenAsSingleString());
        });
        entries.forEach(entry -> {
            Patient patient = (Patient) entry.getResource();
            LOG.info("Patient Name: {} {}, Birthday: {}",
                    patient.getName().get(0).getGivenAsSingleString(),
                    patient.getName().get(0).getFamily(),
                    patient.getBirthDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(patient.getBirthDate()) : null);
        });
    }

    /**
     * Task 2:
     * Print the average response time requesting the patients by the family names from the given file
     */
    public void printAveragePatientLoadingTime(IGenericClient client) {
        ClientInterceptor clientInterceptor = new ClientInterceptor();
        client.registerInterceptor(clientInterceptor);
        List<String> names = loadNamesFromFile(FILE_NAME);

        for (int i = 1; i <= MAX_NUM_LOOPS; i++) {
            boolean noCache = i == MAX_NUM_LOOPS;
            BigDecimal totalTime = names.stream()
                    .map(name -> {
                        findPatientsByFamilyName(client, name, noCache);
                        return new BigDecimal(clientInterceptor.getResponseTime());
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal averageResponseTime = totalTime.divide(new BigDecimal(names.size()), RoundingMode.HALF_UP);
            LOG.info(">> Loop {}, noCache = {}, average Response time: {}ms <<\n", i, noCache, averageResponseTime);
        }
    }

    public List<String> loadNamesFromFile(String fileName) {
        List<String> names = new ArrayList<>();
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(PatientService.class.getResource(fileName)).toURI()));
            for (String name : new String(bytes).split(",")) {
                names.add(name.trim());
            }
            LOG.info("Loaded {} names: {}", names.size(), names);
        } catch (NullPointerException e) {
            throw new NullPointerException("File does not exist");
        } catch (IOException | URISyntaxException e) {
            LOG.error("Failed to read file", e);
            throw new RuntimeException(e);
        }
        return names;
    }

    public Bundle findPatientsByFamilyName(IGenericClient client, String familyName, boolean noCache) {
        Bundle response = client
                .search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().value(familyName))
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(noCache))
                .execute();

        LOG.debug("Found {} patients with family name '{}'", response.getEntry().size(), familyName);
        return response;
    }
}
