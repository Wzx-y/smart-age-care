package care.cloud.care.bed;

public record Bed(
        Long id,
        Long tenantId,
        Long roomId,
        String bedNo,
        BedOccupancyStatus occupancyStatus,
        BedHygieneStatus hygieneStatus,
        long version
) {
}
