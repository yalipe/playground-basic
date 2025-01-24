import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class PatientServiceTest {

    Logger logger = (Logger) LoggerFactory.getLogger(PatientService.class);

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    PatientService patientService;

    @Before
    public void setUp() {
        patientService = new PatientService();
    }

    // Task 1
    @Test
    public void testPrintSortedPatientNames() {
        BundleEntryComponent entry1 = new BundleEntryComponent();
        entry1.setResource(new Patient().addName(new HumanName().addGiven("John").setFamily("Smith")));
        BundleEntryComponent entry2 = new BundleEntryComponent();
        entry2.setResource(new Patient().addName(new HumanName().addGiven("Alice").setFamily("Smith")));
        BundleEntryComponent entry3 = new BundleEntryComponent();
        entry3.setResource(new Patient().addName(new HumanName().addGiven("Bob").setFamily("Smith")));
        List<BundleEntryComponent> entries = Arrays.asList(entry1, entry2, entry3);

        patientService.printSortedPatientNames(entries);

        // The list is sorted by patient given name
        assertEquals("Alice", ((Patient) entries.get(0).getResource()).getName().get(0).getGivenAsSingleString());
        assertEquals("Bob", ((Patient) entries.get(1).getResource()).getName().get(0).getGivenAsSingleString());
        assertEquals("John", ((Patient) entries.get(2).getResource()).getName().get(0).getGivenAsSingleString());
    }

    // Task 2 tests start from here
    @Test
    public void testPrintAveragePatientLoadingTime() {
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        IGenericClient client = setupMockClient();
        patientService.printAveragePatientLoadingTime(client);

        // check log messages
        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals(4, logsList.size());
        assertTrue(StringUtils.contains(logsList.get(0).getFormattedMessage(), "Loaded 20 names"));
        assertTrue(StringUtils.contains(logsList.get(1).getFormattedMessage(), "Loop 1, noCache = false"));
        assertTrue(StringUtils.contains(logsList.get(2).getFormattedMessage(), "Loop 2, noCache = false"));
        assertTrue(StringUtils.contains(logsList.get(3).getFormattedMessage(), "Loop 3, noCache = true"));
    }

    @Test
    public void testLoadNamesFromFile() {
        List<String> names = patientService.loadNamesFromFile(PatientService.FILE_NAME);
        assertEquals(20, names.size());
        assertEquals("Dylan", names.get(0));
        assertEquals("Graves", names.get(1));
        assertEquals("Kumar", names.get(2));
    }

    @Test
    public void testLoadNamesFromFileWithException() {
        exceptionRule.expect(NullPointerException.class);
        exceptionRule.expectMessage("File does not exist");

        patientService.loadNamesFromFile("test.txt");
    }

    @Test
    public void testFindPatientByFamilyName() {
        IGenericClient client = setupMockClient();
        Bundle patientBundle = patientService.findPatientsByFamilyName(client, "Smith", false);
        assertEquals(2, patientBundle.getEntry().size());
    }

    private IGenericClient setupMockClient() {
        IGenericClient client = mock(IGenericClient.class);
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Patient());
        bundle.addEntry().setResource(new Patient());
        when(client.search()).thenReturn(mock(IUntypedQuery.class));
        when(client.search().forResource("Patient")).thenReturn(mock(IQuery.class));
        when(client.search().forResource("Patient").where((ICriterion<?>) any())).thenReturn(mock(IQuery.class));
        when(client.search().forResource("Patient").where((ICriterion<?>) any()).returnBundle(Bundle.class)).thenReturn(mock(IQuery.class));
        when(client.search().forResource("Patient").where((ICriterion<?>) any()).returnBundle(Bundle.class).cacheControl(any())).thenReturn(mock(IQuery.class));
        when(client.search().forResource("Patient").where((ICriterion<?>) any()).returnBundle(Bundle.class).cacheControl(any()).execute()).thenReturn(bundle);

        return client;
    }

}