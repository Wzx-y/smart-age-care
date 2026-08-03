package care.cloud.care.export;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SupabaseExportFileStorage implements ExportFileStorage {
    private final RestClient restClient = RestClient.create();
    private final String baseUrl;
    private final String bucket;
    private final String serviceKey;

    public SupabaseExportFileStorage(@Value("${exports.storage.base-url:}") String baseUrl,
                                     @Value("${exports.storage.bucket:}") String bucket,
                                     @Value("${exports.storage.service-key:}") String serviceKey) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/$", "");
        this.bucket = bucket;
        this.serviceKey = serviceKey;
    }

    @Override
    public void store(String storageKey, byte[] content) {
        requireConfiguration();
        try {
            restClient.put().uri(storageUri("object", storageKey)).header("apikey", serviceKey)
                    .header("Authorization", "Bearer " + serviceKey).contentType(MediaType.valueOf("text/csv"))
                    .body(content).retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            throw new ExportStorageUnavailableException(exception);
        }
    }

    @Override
    public ExportAccessTarget signDownload(String storageKey) {
        requireConfiguration();
        try {
            SignedDownloadResponse response = restClient.post().uri(storageUri("object/sign", storageKey))
                    .header("apikey", serviceKey).header("Authorization", "Bearer " + serviceKey)
                    .contentType(MediaType.APPLICATION_JSON).body(Map.of("expiresIn", 300, "download", true))
                    .retrieve().body(SignedDownloadResponse.class);
            if (response == null || !StringUtils.hasText(response.signedURL())) throw new ExportStorageUnavailableException();
            return new ExportAccessTarget(baseUrl + "/storage/v1" + response.signedURL(), OffsetDateTime.now().plusMinutes(5));
        } catch (RestClientException exception) {
            throw new ExportStorageUnavailableException(exception);
        }
    }

    private URI storageUri(String operation, String storageKey) {
        return UriComponentsBuilder.fromUriString(baseUrl).pathSegment("storage", "v1")
                .path("/" + operation + "/" + bucket + "/" + storageKey).build().toUri();
    }

    private void requireConfiguration() {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(bucket) || !StringUtils.hasText(serviceKey)) throw new ExportStorageUnavailableException();
    }

    private record SignedDownloadResponse(String signedURL) {
    }
}
