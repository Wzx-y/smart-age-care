package care.cloud.care.resident;

import static org.junit.jupiter.api.Assertions.assertEquals;

import care.cloud.care.security.TenantContext;
import care.cloud.care.security.TenantPrincipal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResidentStatusTest {
    @Test
    void newResidentUsesCurrentTenantAndPendingAdmissionState() {
        InMemoryResidentRepository repository = new InMemoryResidentRepository();
        ResidentService service = new ResidentService(
                repository,
                org.mockito.Mockito.mock(ResidentProfileRepository.class),
                org.mockito.Mockito.mock(ResidentAttachmentStorage.class)
        );
        TenantContext.set(new TenantPrincipal(101L, 9001L));

        try {
            Resident resident = service.create(new CreateResidentRequest(
                    "王素兰", "女", LocalDate.of(1944, 5, 12), "王敏", "13800000000"
            ));

            assertEquals(9001L, resident.tenantId());
            assertEquals(ResidentStatus.PENDING_ADMISSION, resident.status());
        } finally {
            TenantContext.clear();
        }
    }

    private static class InMemoryResidentRepository implements ResidentRepository {
        private final List<Resident> residents = new ArrayList<>();

        @Override
        public Resident save(Resident resident) {
            Resident saved = new Resident(
                    (long) residents.size() + 1,
                    resident.tenantId(), resident.name(), resident.gender(), resident.birthDate(),
                    resident.status(), resident.currentBedId(), resident.emergencyContactName(),
                    resident.emergencyContactPhone(), resident.createdAt(), resident.updatedAt()
            );
            residents.add(saved);
            return saved;
        }

        @Override
        public boolean update(Resident resident) {
            return true;
        }

        @Override
        public boolean archive(Long id, Long tenantId, java.time.OffsetDateTime archivedAt) {
            return true;
        }

        @Override
        public Optional<Resident> findByIdAndTenantId(Long id, Long tenantId) {
            return residents.stream().filter(item -> item.id().equals(id) && item.tenantId().equals(tenantId)).findFirst();
        }

        @Override
        public List<Resident> findByTenantId(Long tenantId, String keyword) {
            return residents.stream().filter(item -> item.tenantId().equals(tenantId)).toList();
        }
    }
}
