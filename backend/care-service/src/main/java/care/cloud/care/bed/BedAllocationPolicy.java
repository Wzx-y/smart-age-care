package care.cloud.care.bed;

import care.cloud.care.admission.Admission;
import care.cloud.care.admission.AdmissionStatus;

public final class BedAllocationPolicy {
    private BedAllocationPolicy() {
    }

    public static void requireAssignable(Admission admission, Bed bed) {
        if (!admission.tenantId().equals(bed.tenantId())) {
            throw new BedAllocationException("入住单与床位不属于同一机构租户");
        }
        if (admission.status() != AdmissionStatus.PENDING_ASSIGNMENT) {
            throw new BedAllocationException("当前入住单不处于待分配状态");
        }
        if (bed.occupancyStatus() != BedOccupancyStatus.AVAILABLE) {
            throw new BedAllocationException("床位当前不可分配");
        }
        if (bed.hygieneStatus() != BedHygieneStatus.READY) {
            throw new BedAllocationException("床位尚未完成卫生准备");
        }
    }

    public static void requireConfirmable(Admission admission, Bed bed) {
        if (!admission.tenantId().equals(bed.tenantId())) {
            throw new BedAllocationException("入住单与床位不属于同一机构租户");
        }
        if (admission.bedId() == null || !admission.bedId().equals(bed.id())) {
            throw new BedAllocationException("入住单未绑定当前床位");
        }
        if (admission.status() != AdmissionStatus.PENDING_CONFIRMATION) {
            throw new BedAllocationException("当前入住单不处于待确认状态");
        }
        if (bed.occupancyStatus() != BedOccupancyStatus.RESERVED) {
            throw new BedAllocationException("床位当前未处于预留状态");
        }
        if (bed.hygieneStatus() != BedHygieneStatus.READY) {
            throw new BedAllocationException("床位尚未完成卫生准备");
        }
    }

    public static void requireTransferable(Admission admission, Bed source, Bed target) {
        if (!admission.tenantId().equals(source.tenantId()) || !admission.tenantId().equals(target.tenantId())
                || admission.status() != AdmissionStatus.ADMITTED || !source.id().equals(admission.bedId())
                || source.occupancyStatus() != BedOccupancyStatus.OCCUPIED
                || target.occupancyStatus() != BedOccupancyStatus.AVAILABLE
                || target.hygieneStatus() != BedHygieneStatus.READY) {
            throw new BedAllocationException("当前入住状态或目标床位不满足调床条件");
        }
    }
}
