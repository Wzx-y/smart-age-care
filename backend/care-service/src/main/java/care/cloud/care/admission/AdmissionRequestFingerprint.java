package care.cloud.care.admission;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class AdmissionRequestFingerprint {
    private AdmissionRequestFingerprint() {
    }

    public static String forAssignBed(Long admissionId, AssignBedRequest request) {
        return sha256("ASSIGN_BED|" + admissionId + "|" + request.bedId() + "|" + request.admissionVersion() + "|" + request.bedVersion());
    }

    public static String forCreate(CreateAdmissionRequest request) {
        return sha256("CREATE_ADMISSION|" + request.residentId());
    }

    public static String forConfirm(Long admissionId, ConfirmAdmissionRequest request) {
        return sha256("CONFIRM_ADMISSION|" + admissionId + "|" + request.admissionVersion() + "|" + request.bedVersion());
    }

    public static String forDischarge(Long admissionId, DischargeAdmissionRequest request) {
        return sha256("DISCHARGE_ADMISSION|" + admissionId + "|" + request.admissionVersion() + "|" + request.bedVersion() + "|" + request.dischargeReason());
    }

    public static String forCancel(Long admissionId, CancelAdmissionRequest request) {
        return sha256("CANCEL_ADMISSION|" + admissionId + "|" + request.admissionVersion() + "|" + request.bedVersion());
    }

    public static String forAssessment(Long admissionId, DecideAdmissionAssessmentRequest request) {
        return sha256("DECIDE_ADMISSION_ASSESSMENT|" + admissionId + "|" + request.admissionVersion()
                + "|" + request.decision() + "|" + request.conclusion());
    }

    public static String forTransfer(Long admissionId, TransferBedRequest request) {
        return sha256("TRANSFER_ADMISSION_BED|" + admissionId + "|" + request.targetBedId() + "|"
                + request.admissionVersion() + "|" + request.sourceBedVersion() + "|" + request.targetBedVersion()
                + "|" + request.reason());
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境不支持 SHA-256", exception);
        }
    }
}
