package care.cloud.care.masterdata;

public class MasterDataNotFoundException extends RuntimeException {
    public MasterDataNotFoundException(String resourceType, Long id) {
        super(resourceType + " not found: " + id);
    }
}
