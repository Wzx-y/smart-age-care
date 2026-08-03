package care.cloud.care.resident;

import care.cloud.care.admission.AdmissionSummary;
import care.cloud.care.care.CarePlan;
import care.cloud.care.care.CareTask;
import care.cloud.care.care.CareServiceRecord;
import java.util.List;

public record Resident360View(
        Resident resident, ResidentHealthProfile healthProfile, List<ResidentContact> contacts,
        List<ResidentAssessment> assessments, List<ResidentAttachment> attachments,
        List<AdmissionSummary> admissions, List<CarePlan> carePlans, List<CareTask> careTasks,
        List<CareServiceRecord> serviceRecords
) {
}
