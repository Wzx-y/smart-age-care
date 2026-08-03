package care.cloud.care.shared;

import care.cloud.care.admission.AdmissionNotFoundException;
import care.cloud.care.admission.AdmissionVersionConflictException;
import care.cloud.care.admission.BedNotFoundException;
import care.cloud.care.admission.IdempotencyKeyConflictException;
import care.cloud.care.admission.IdempotencyRequestInProgressException;
import care.cloud.care.admission.InvalidIdempotencyKeyException;
import care.cloud.care.bed.BedAllocationException;
import care.cloud.care.care.CareNotFoundException;
import care.cloud.care.care.CareStateConflictException;
import care.cloud.care.care.CareIdempotencyKeyConflictException;
import care.cloud.care.care.CareIdempotencyRequestInProgressException;
import care.cloud.care.care.CareInvalidIdempotencyKeyException;
import care.cloud.care.care.CareShiftHandoverConflictException;
import care.cloud.care.export.ExportConflictException;
import care.cloud.care.export.ExportNotFoundException;
import care.cloud.care.export.ExportStorageUnavailableException;
import care.cloud.care.masterdata.MasterDataConflictException;
import care.cloud.care.masterdata.MasterDataNotFoundException;
import care.cloud.care.notification.NotificationNotFoundException;
import care.cloud.care.resident.ResidentNotFoundException;
import care.cloud.care.resident.ResidentArchiveConflictException;
import care.cloud.care.resident.ResidentAttachmentStorageUnavailableException;
import care.cloud.care.resident.ResidentAttachmentUploadConflictException;
import care.cloud.care.resident.ResidentProfileConflictException;
import care.cloud.care.security.TenantContextMissingException;
import care.cloud.care.security.CareAccessDeniedException;
import care.cloud.care.security.MemberDirectoryUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResidentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResidentNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({AdmissionNotFoundException.class, BedNotFoundException.class, MasterDataNotFoundException.class, ExportNotFoundException.class, NotificationNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleAdmissionNotFound(RuntimeException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(CareNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCareNotFound(CareNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({BedAllocationException.class, AdmissionVersionConflictException.class, IdempotencyKeyConflictException.class, IdempotencyRequestInProgressException.class, CareStateConflictException.class, CareIdempotencyKeyConflictException.class, CareIdempotencyRequestInProgressException.class, CareShiftHandoverConflictException.class, ResidentArchiveConflictException.class, ResidentAttachmentUploadConflictException.class, ResidentProfileConflictException.class, MasterDataConflictException.class, ExportConflictException.class})
    public ResponseEntity<ApiResponse<Void>> handleConflict(RuntimeException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidIdempotencyKey(InvalidIdempotencyKeyException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(CareInvalidIdempotencyKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCareIdempotencyKey(CareInvalidIdempotencyKeyException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ResidentAttachmentStorageUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleAttachmentStorageUnavailable(ResidentAttachmentStorageUnavailableException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(ExportStorageUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleExportStorageUnavailable(ExportStorageUnavailableException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(TenantContextMissingException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantContext(TenantContextMissingException exception) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(CareAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCareAccessDenied(CareAccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(MemberDirectoryUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMemberDirectoryUnavailable(MemberDirectoryUnavailableException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("请求参数不正确");
        return response(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<ApiResponse<Void>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.failure(status.value(), message));
    }
}
