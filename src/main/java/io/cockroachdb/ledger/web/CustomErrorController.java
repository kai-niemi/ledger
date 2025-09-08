package io.cockroachdb.ledger.web;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Error handler for everything not captured by front-end or API global error handlers.
 * Normally this would go to the container white-label page or the
 * SpringMVC default error handler.
 *
 * @author Kai Niemi
 */
@Controller
public class CustomErrorController implements ErrorController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @RequestMapping("/error")
    public ModelAndView handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object exceptionType = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
        Object servletName = request.getAttribute(RequestDispatcher.ERROR_SERVLET_NAME);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        HttpStatus httpStatus;
        if (status != null) {
            httpStatus = HttpStatus.valueOf(Integer.parseInt(status.toString()));
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("message", message);
        model.put("status", httpStatus);
        model.put("uri", requestUri);
        model.put("servletName", servletName);
        model.put("exception", exception);
        model.put("exceptionType", exceptionType);
        model.put("timestamp", Instant.now().toString());

        return new ModelAndView("error", model);
    }

    //    @RequestMapping("/error")
    public ResponseEntity<Object> handleError2(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object exceptionType = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
        Object servletName = request.getAttribute(RequestDispatcher.ERROR_SERVLET_NAME);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        HttpStatus httpStatus;
        if (status != null) {
            httpStatus = HttpStatus.valueOf(Integer.parseInt(status.toString()));
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ProblemDetail problem = ProblemDetail.forStatus(httpStatus);
        problem.setDetail(message != null ? "" + message : null);
        problem.setTitle(httpStatus.getReasonPhrase());
        if (!Objects.isNull(requestUri)) {
            problem.setProperty("uri", requestUri);
        }
        if (!Objects.isNull(servletName)) {
            problem.setProperty("servletName", servletName);
        }
        if (!Objects.isNull(exception)) {
            problem.setProperty("exception", exception);
        }
        if (!Objects.isNull(exceptionType)) {
            problem.setProperty("exceptionType", exceptionType);
        }
        problem.setProperty("timestamp", Instant.now().toString());

        if (httpStatus.is5xxServerError()) {
            logger.error("Server error: " + problem);
//        } else if (httpStatus.is4xxClientError()) {
//            logger.warn("Client error: " + problem);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        return ResponseEntity
                .of(problem)
                .headers(headers)
                .build();
    }
}
