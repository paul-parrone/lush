package com.px3j.lush.web;

import brave.baggage.BaggageField;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.px3j.lush.core.exception.LushException;
import com.px3j.lush.core.exception.StackTraceToLoggerWriter;
import com.px3j.lush.core.model.LushAdvice;
import com.px3j.lush.core.model.LushContext;
import com.px3j.lush.core.ticket.LushTicket;
import com.px3j.lush.web.common.Constants;
import com.px3j.lush.web.common.ControllerDecorator;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Objects;

@Aspect
@Component
@Slf4j(topic = "lush.core.debug")
public class WebControllerDecorator extends ControllerDecorator {
    private final ObjectMapper objectMapper;

    public WebControllerDecorator(//BaggageField lushUserNameField,
                                  Tracer tracer, ObjectMapper objectMapper) {
        super(null, tracer);
        this.objectMapper = objectMapper;
    }

    @Around("lushControllerMethods() && execution(public org.springframework.http.ResponseEntity *..*(..))")
    public ResponseEntity<?> invocationAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("****");
            log.debug("intercepted request - web invocation");
        }

        BaggageInScope scope = null;

        try {
            HttpServletRequest request = getRequest();
            LushTicket ticket = getLushTicket();
            LushContext lushContext = setLushContext(request);
            scope = this.tracer.createBaggageInScope("lush-user-name", ticket.getUsername());

            if (log.isDebugEnabled()) log.debug("ticket user: " + ticket.getUsername());
//            lushUserNameField.updateValue(ticket.getUsername());
            MDC.put("lush-user-name", ticket.getUsername());

            ResponseEntity ogResponse = (ResponseEntity) invokeControllerMethod(joinPoint, lushContext, ticket);
            return getMutatedResponse(ogResponse, lushContext);
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
        finally {
            MDC.remove("lush-user-name");
            if (scope != null) scope.close();
            if (log.isDebugEnabled()) log.debug("****");
        }
    }

    protected Object invokeControllerMethod(ProceedingJoinPoint pjp, LushContext apiContext, LushTicket ticket) {
        try {
            // Get the target method from the join point, use this to inject parameters.
            Method method = getMethodBeingCalled(pjp);

            // If the method declares an argument of LushContext, inject it (we inject it by copying the values)
            injectLushContext(method, pjp, apiContext);
            // If the method declares an argument of LushTicket, inject it (we inject it by copying the values)
            injectTicket(method, pjp, ticket);

            // Proceed with the target method
            return pjp.proceed();
        }
        catch (final Throwable throwable) {
            errorHandler(apiContext, throwable);
            return ResponseEntity.ok().build();
        }
    }


    /**
     * Sets the LushContext for the given HttpServletRequest. This method generates
     * a trace ID, creates a LushAdvice object with a default status, assigns these
     * objects to a new LushContext, and sets this context as an attribute in the
     * request.
     *
     * @param request the HttpServletRequest for which the LushContext needs to be set
     * @return the created LushContext object containing the trace ID and advice
     */
    private LushContext setLushContext(HttpServletRequest request) {
        String requestKey = generateTraceId(tracer);
        LushAdvice advice = new LushAdvice(requestKey, 200);
        LushContext lushContext = new LushContext();

        lushContext.setTraceId(requestKey);
        lushContext.setAdvice(advice);
        request.setAttribute("lushContext", lushContext);
        return lushContext;
    }

    /**
     * Retrieves the current LushTicket from the security context.
     * <p>
     * This method extracts the authentication details from the security context
     * and ensures that the method is executed in a secured context with a valid
     * LushTicket. It throws an exception if the method is invoked in an unsecured
     * context or if the LushTicket is not available.
     *
     * @return the current LushTicket associated with the authenticated user
     * @throws LushException if the method is invoked in a non-secured context or if the LushTicket is not available
     */
    private static LushTicket getLushTicket() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new LushException("@LushControllerMethod should not be applied to a non-secured method");
        }

        LushTicket ticket = (LushTicket) authentication.getPrincipal();
        if (ticket == null) {
            throw new LushException("@LushControllerMethod should not be mapped to a non HTTP request");
        }
        return ticket;
    }

    /**
     * Retrieves the current HTTP servlet request from the request context.
     * If the request attributes are not available, throws a LushException indicating
     * that the method should not be mapped to a non-HTTP request.
     *
     * @return the current HttpServletRequest
     * @throws LushException if the current request is not an HTTP request
     */
    private static HttpServletRequest getRequest() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra == null) {
            throw new LushException("@LushControllerMethod should not be mapped to a non HTTP request");
        }
        return sra.getRequest();
    }

    private ResponseEntity<Object> getMutatedResponse(ResponseEntity ogResponse, LushContext lushContext) throws JsonProcessingException {
        HttpHeaders newHeaders = new HttpHeaders();
        ogResponse.getHeaders()
                .forEach((key, value) -> newHeaders.add(key, value.get(0)));

        newHeaders.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, Constants.ADVICE_HEADER_NAME);
        newHeaders.add(Constants.ADVICE_HEADER_NAME, objectMapper.writeValueAsString(lushContext.getAdvice()));

        ResponseEntity<Object> mutatedResponse = new ResponseEntity<>(
                ogResponse.getBody(),
                newHeaders,
                ogResponse.getStatusCode()
        );
        return mutatedResponse;
    }

    private String generateTraceId(Tracer tracer) {
        String contextKey = "?/?";

        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            contextKey = currentSpan.context().traceId() + "," + currentSpan.context().spanId();
        }

        return contextKey;
    }
}
