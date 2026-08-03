package care.cloud.care.masterdata;

import care.cloud.care.bed.BedHygieneStatus;
import care.cloud.care.bed.BedOccupancyStatus;

public record ManagedBed(Long id, Long tenantId, Long roomId, String roomNo, String bedNo,
                         String equipmentSummary, BedOccupancyStatus occupancyStatus,
                         BedHygieneStatus hygieneStatus, boolean enabled, long version) {
}
