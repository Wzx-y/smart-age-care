package care.cloud.care.admission;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface AdmissionRepository {
    Admission create(Long tenantId, Long residentId, Long userId);
    Optional<Admission> findByIdAndTenantId(Long id, Long tenantId);

    List<AdmissionSummary> findByTenantId(Long tenantId);

    boolean assignBed(Admission admission, Long bedId, OffsetDateTime reservedUntil, Long userId);

    boolean decideAssessment(Admission admission, AdmissionStatus nextStatus, DecideAdmissionAssessmentRequest request, Long userId);

    boolean confirm(Admission admission, Long userId);

    boolean discharge(Admission admission, Long userId, String dischargeReason);

    boolean transferBed(Admission admission, TransferBedRequest request, Long userId);

    boolean cancel(Admission admission, Long userId);

    List<Admission> findExpiredReservations(OffsetDateTime now);

    boolean releaseExpiredReservation(Admission admission, Long userId);

    List<AdmissionAuditEvent> findAuditEvents(Long tenantId, Long admissionId);
}
