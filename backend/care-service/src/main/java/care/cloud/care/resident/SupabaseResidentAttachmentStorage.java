package care.cloud.care.resident;

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
public class SupabaseResidentAttachmentStorage implements ResidentAttachmentStorage {
    private final RestClient restClient;
    private final String baseUrl;
    private final String bucket;
    private final String serviceKey;

    public SupabaseResidentAttachmentStorage(
            @Value("${attachments.storage.base-url:}") String baseUrl,
            @Value("${attachments.storage.bucket:}") String bucket,
            @Value("${attachments.storage.service-key:}") String serviceKey
    ) {
        this.restClient = RestClient.create();
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/$", "");
        this.bucket = bucket;
        this.serviceKey = serviceKey;
    }

    @Override
    public ResidentAttachmentUploadTarget signUpload(String storageKey, String contentType) {
        requireConfiguration();
        try {
            SignedUploadResponse response = restClient.post()
                    .uri(storageUri("object/upload/sign", storageKey))
                    .header("apikey", serviceKey)
                    .header("Authorization", "Bearer " + serviceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("upsert", false))
                    .retrieve()
                    .body(SignedUploadResponse.class);
            if (response == null || !StringUtils.hasText(response.url())) {
                throw new ResidentAttachmentStorageUnavailableException();
            }
            return new ResidentAttachmentUploadTarget(baseUrl + "/storage/v1" + response.url(), OffsetDateTime.now().plusMinutes(10));
        } catch (RestClientException exception) {
            throw new ResidentAttachmentStorageUnavailableException(exception);
        }
    }

    @Override
    public ResidentAttachmentAccessTarget signDownload(String storageKey, boolean inline) {
        requireConfiguration();
        try {
            SignedDownloadResponse response = restClient.post()
                    .uri(storageUri("object/sign", storageKey))
                    .header("apikey", serviceKey)
                    .header("Authorization", "Bearer " + serviceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", 300, "download", !inline))
                    .retrieve()
                    .body(SignedDownloadResponse.class);
            if (response == null || !StringUtils.hasText(response.signedURL())) throw new ResidentAttachmentStorageUnavailableException();
            return new ResidentAttachmentAccessTarget(baseUrl + "/storage/v1" + response.signedURL(), OffsetDateTime.now().plusMinutes(5));
        } catch (RestClientException exception) {
            throw new ResidentAttachmentStorageUnavailableException(exception);
        }
    }

    @Override
    public StoredObject inspect(String storageKey) {
        requireConfiguration();
        try {
            ObjectInfoResponse response = restClient.get()
                    .uri(storageUri("object/info", storageKey))
                    .header("apikey", serviceKey)
                    .header("Authorization", "Bearer " + serviceKey)
                    .retrieve()
                    .body(ObjectInfoResponse.class);
            if (response == null || response.metadata() == null) {
                throw new ResidentAttachmentStorageUnavailableException();
            }
            Object size = response.metadata().get("size");
            Object contentType = response.metadata().get("mimetype");
            if (size == null || contentType == null) {
                throw new ResidentAttachmentStorageUnavailableException();
            }
            return new StoredObject(String.valueOf(contentType), Long.parseLong(String.valueOf(size)));
        } catch (RestClientException | NumberFormatException exception) {
            throw new ResidentAttachmentStorageUnavailableException(exception);
        }
    }

    private URI storageUri(String operation, String storageKey) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("storage", "v1")
                .path("/" + operation + "/" + bucket + "/" + storageKey)
                .build()
                .toUri();
    }

    private void requireConfiguration() {
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(bucket) || !StringUtils.hasText(serviceKey)) {
            throw new ResidentAttachmentStorageUnavailableException();
        }
    }

    private record SignedUploadResponse(String url) {
    }

    private record SignedDownloadResponse(String signedURL) {
    }

    private record ObjectInfoResponse(Map<String, Object> metadata) {
    }
}
