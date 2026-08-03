package care.cloud.care.masterdata;

public record MasterDataItem(Long id, Long tenantId, MasterDataCategory category, String itemCode,
                             String itemName, String description, int sortOrder, boolean enabled, long version) {
}
