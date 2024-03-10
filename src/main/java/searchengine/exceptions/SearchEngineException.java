package searchengine.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SearchEngineException extends RuntimeException {

    private final HttpStatus status;

    public SearchEngineException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

}
