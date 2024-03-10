package searchengine.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.error.ErrorMessageDto;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ErrorMessageDto> catchSearchEngineException(
            SearchEngineException e) {
        return new ResponseEntity<>(new ErrorMessageDto(e.getMessage()), e.getStatus());
    }

}
