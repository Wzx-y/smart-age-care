package care.cloud.care.admission;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdmissionReservationExpiryScheduler {
    private final AdmissionReservationExpiryService expiryService;

    public AdmissionReservationExpiryScheduler(AdmissionReservationExpiryService expiryService) {
        this.expiryService = expiryService;
    }

    @Scheduled(fixedDelayString = "${admission.reservation.expiry-scan-ms:60000}")
    public void releaseExpiredReservations() {
        expiryService.releaseExpiredReservations();
    }
}
