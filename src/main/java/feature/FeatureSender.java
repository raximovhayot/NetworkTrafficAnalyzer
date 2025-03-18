package feature;

import flow.Flow;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import com.fasterxml.jackson.databind.ObjectMapper;


public class FeatureSender {
    private static final String SPRING_API_URL = "http://localhost:8080/api/flows";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void sendFeatures(Flow flow) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(SPRING_API_URL);
            post.setEntity(new StringEntity(mapper.writeValueAsString(flow.getFeatures())));
            post.setHeader("Content-Type", "application/json");
            client.execute(post);
        } catch (Exception e) {
            System.err.println("Error sending features: " + e.getMessage());
        }
    }
}