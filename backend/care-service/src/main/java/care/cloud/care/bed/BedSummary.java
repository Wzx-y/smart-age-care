package care.cloud.care.bed;

public record BedSummary(
        Long id,
        Long roomId,
        String roomNo,
        String roomType,
        String bedNo,
        BedOccupancyStatus occupancyStatus,
        BedHygieneStatus hygieneStatus,
        long version
) {
}
