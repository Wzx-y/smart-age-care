package care.cloud.care.admission;

public interface AdmissionAuditRepository {
    void recordConfirmed(Long tenantId, Long actorId, Long admissionId, Long residentId, Long bedId);

    void recordDischarged(Long tenantId, Long actorId, Long admissionId, Long residentId, Long bedId);

    void recordCancelled(Long tenantId, Long actorId, Long admissionId, Long residentId, Long bedId);

    void recordReservationExpired(Long tenantId, Long admissionId, Long residentId, Long bedId);

    void recordAssessmentDecided(Long tenantId, Long actorId, Long admissionId, Long residentId, AdmissionAssessmentDecisionStatus decision);

    void recordBedTransferred(Long tenantId, Long actorId, Long admissionId, Long residentId, Long sourceBedId, Long targetBedId);

    void recordCleaningCompleted(Long tenantId, Long actorId, Long bedId);
}
