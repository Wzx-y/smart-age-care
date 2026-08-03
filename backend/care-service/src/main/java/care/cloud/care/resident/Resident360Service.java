package care.cloud.care.resident;

import care.cloud.care.admission.AdmissionRepository;
import care.cloud.care.care.CarePlanRepository;
import care.cloud.care.care.CareTaskRepository;
import care.cloud.care.care.CareServiceRecordQueryService;
import care.cloud.care.security.TenantContext;
import java.util.Comparator;
import org.springframework.stereotype.Service;

@Service
public class Resident360Service {
    private final ResidentService residentService;
    private final AdmissionRepository admissions;
    private final CarePlanRepository carePlans;
    private final CareTaskRepository careTasks;
    private final CareServiceRecordQueryService serviceRecords;

    public Resident360Service(ResidentService residentService, AdmissionRepository admissions,
                              CarePlanRepository carePlans, CareTaskRepository careTasks,
                              CareServiceRecordQueryService serviceRecords) {
        this.residentService = residentService;
        this.admissions = admissions;
        this.carePlans = carePlans;
        this.careTasks = careTasks;
        this.serviceRecords = serviceRecords;
    }

    public Resident360View get(Long residentId) {
        Resident resident = residentService.get(residentId);
        Long tenantId = TenantContext.requireCurrent().tenantId();
        return new Resident360View(
                resident, residentService.getHealthProfile(residentId), residentService.listContacts(residentId),
                residentService.listAssessments(residentId), residentService.listAttachments(residentId),
                admissions.findByTenantId(tenantId).stream().filter(item -> item.residentId().equals(residentId)).toList(),
                carePlans.findByTenantId(tenantId).stream().filter(item -> item.residentId().equals(residentId))
                        .sorted(Comparator.comparingLong(item -> item.version())).toList(),
                careTasks.findByTenantId(tenantId).stream().filter(item -> item.residentId().equals(residentId))
                        .sorted(Comparator.comparing(item -> item.scheduledAt(), Comparator.reverseOrder())).toList(),
                serviceRecords.list(residentId, null, null)
        );
    }
}
