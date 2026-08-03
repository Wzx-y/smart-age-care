package care.cloud.care.resident;

public interface ResidentAdmissionRepository {
    boolean markAdmitted(Long residentId, Long tenantId, Long bedId, Long userId);

    boolean markDischarged(Long residentId, Long tenantId, Long bedId, Long userId);

    boolean moveBed(Long residentId, Long tenantId, Long sourceBedId, Long targetBedId, Long userId);
}
