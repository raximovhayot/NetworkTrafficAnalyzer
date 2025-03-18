package feature;

import flow.Flow;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FeatureSender {
    private static final Logger log = LoggerFactory.getLogger(FeatureSender.class);

    private static final String SPRING_API_URL = "http://localhost:8080/api/flows";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void sendFeatures(Flow flow) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(SPRING_API_URL);
            post.setEntity(new StringEntity(mapper.writeValueAsString(flow.getFeatures())));
            post.setHeader("Content-Type", "application/json");
            client.execute(post);
        } catch (Exception e) {
            log.error("Error sending features: {}", e.getMessage());
        }
    }
}