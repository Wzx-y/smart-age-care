package care.cloud.care.shared;

public record ApiResponse<T>(int code, String message, T data, String traceId) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "ok", data, null);
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }
}
