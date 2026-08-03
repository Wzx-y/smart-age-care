package care.cloud.care.resident;

public record ResidentContact(Long id, Long residentId, String name, String relationshipText, String phone,
                              boolean primaryContact, long version) {
}
