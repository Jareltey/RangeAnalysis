package common;

public enum ErrorMessage {
    NEGATIVE_INDEX_ERROR("negative array index"),
    POSSIBLE_NEGATIVE_INDEX_WARNING("array index may be negative"),
    EXCEED_ARRAY_LENGTH_ERROR("array index exceeds array length"),
    POSSIBLE_EXCEED_ARRAY_LENGTH_WARNING("array index may exceed array length"),
    EITHER_NEGATIVE_INDEX_OR_EXCEED_ARRAY_LENGTH_WARNING("array index may be negative or exceed array length");

    ErrorMessage(String message) {
        this.errorMessage = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private String errorMessage;
}
