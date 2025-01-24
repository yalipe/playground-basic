import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.util.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Interceptor
public class ClientInterceptor implements IClientInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(ClientInterceptor.class);

    private long responseTime = 0;

    @Hook(Pointcut.CLIENT_REQUEST)
    public void interceptRequest(IHttpRequest iHttpRequest) {
        LOG.debug("Request URL: {}", iHttpRequest.getUri());
    }

    @Hook(Pointcut.CLIENT_RESPONSE)
    public void interceptResponse(IHttpResponse iHttpResponse) throws IOException {
        LOG.debug("Response Status: {}", iHttpResponse.getStatus());
        StopWatch requestStopWatch = iHttpResponse.getRequestStopWatch();
        responseTime = requestStopWatch.getMillis();
    }

    public long getResponseTime() {
        return responseTime;
    }
}
