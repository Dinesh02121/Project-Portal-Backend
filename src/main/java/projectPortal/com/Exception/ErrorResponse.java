package projectPortal.com.Exception;

import lombok.Data;

@Data
public class ErrorResponse {
    private int status;
    private String message;

    public ErrorResponse(int value, String message) {
        this.message=message;
        this.status=value;
    }

}
