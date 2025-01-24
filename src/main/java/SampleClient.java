import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.hl7.fhir.r4.model.Bundle;

public class SampleClient {

    public static void main(String[] theArgs) {

        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));

        PatientService patientService = new PatientService();
        Bundle response = patientService.findPatientsByFamilyName(client, "SMITH", false);
        // Task 1
        patientService.printSortedPatientNames(response.getEntry());
        // Task 2
        patientService.printAveragePatientLoadingTime(client);
    }

}
