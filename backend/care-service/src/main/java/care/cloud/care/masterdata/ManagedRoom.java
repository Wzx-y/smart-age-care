package care.cloud.care.masterdata;

public record ManagedRoom(Long id, Long tenantId, String building, String floor, String roomNo,
                          String roomType, String nursingUnit, boolean enabled, long version) {
}
