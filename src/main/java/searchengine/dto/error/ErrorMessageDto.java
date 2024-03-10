package searchengine.dto.error;

import lombok.Getter;

@Getter
public class ErrorMessageDto {
    boolean result;
    String error;

    public ErrorMessageDto(String error) {
        result = false;
        this.error = error;
    }
}
