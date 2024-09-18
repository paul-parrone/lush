package com.px3j.lush.webflux;

import brave.baggage.BaggageField;
import com.px3j.lush.core.exception.StackTraceToLoggerWriter;
import com.px3j.lush.core.model.LushAdvice;
import com.px3j.lush.core.model.LushContext;
import com.px3j.lush.core.ticket.LushTicket;
import com.px3j.lush.web.common.ControllerDecorator;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.reactivestreams.Publisher;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

/**
 * Decorator, applied via AOP, that intercepts calls to any Lush based controllers.  It silently intercepts and provides
 * the Lush functionality.
 *
 * @author Paul Parrone
 */
@Aspect
@Component
@Slf4j(topic = "lush.core.debug")
public class ReactiveControllerDecorator extends ControllerDecorator {
    @Autowired
    public ReactiveControllerDecorator(BaggageField lushUserNameField, Tracer tracer) {
        super(lushUserNameField, tracer);
    }

    @Around("lushControllerMethods() && execution(public reactor.core.publisher.Mono *..*(..))")
    public Mono monoInvocationAdvice(ProceedingJoinPoint pjp) {
        if (log.isDebugEnabled()) {
            log.debug("****");
            log.debug("intercepted request - Mono invocation");
        }

        return Mono.deferContextual(ctx -> {
            LushContext lushContext = ctx.get(LushContext.class.getName());
            log.debug("LushContext: " + lushContext);

            return ReactiveSecurityContextHolder.getContext()
                    .map(sc -> (LushTicket) sc.getAuthentication().getPrincipal())
                    .map(ticket -> {
                        if (log.isDebugEnabled()) log.debug("ticket user: " + ticket.getUsername());
//                        tracer.createBaggageInScope( "lush-user-name", ticket.getUsername());
                        lushUserNameField.updateValue(ticket.getUsername());
                        MDC.put("lush-user-name", ticket.getUsername());
                        return ticket;
                    })
                    .flatMap((ticket) -> (Mono) decoratorImpl(pjp, lushContext, ticket, false))
                    .doFinally( signal -> {
                        MDC.remove("lush-user-name");
                    });
        });
    }

    @Around("lushControllerMethods() && execution(public reactor.core.publisher.Flux *..*(..))")
    public Flux fluxInvocationAdvice(ProceedingJoinPoint pjp) {
        if (log.isDebugEnabled()) {
            log.debug("****");
            log.debug("intercepted request - Flux invocation");
        }

        return Flux.deferContextual(ctx -> {
            LushContext lushContext = ctx.get(LushContext.class.getName());
            log.debug("LushContext: " + lushContext);

            return ReactiveSecurityContextHolder.getContext()
                    .map(sc -> (LushTicket) sc.getAuthentication().getPrincipal())
                    .map(ticket -> {
                        if (log.isDebugEnabled()) log.debug("ticket user: " + ticket.getUsername());
//                        lushUserNameField.updateValue(ticket.getUsername());
                        return ticket;
                    })
                    .flatMapMany((ticket) -> (Flux) decoratorImpl(pjp, lushContext, ticket, true));
        });
    }

    protected Object decoratorImpl(ProceedingJoinPoint pjp, LushContext apiContext, LushTicket ticket, boolean fluxOnError) {
//        CarryingContext apiContext = (CarryingContext)ThreadLocalApiContext.get();

        try {
            // Get the target method from the join point, use this to inject parameters.
            Method method = getMethodBeingCalled(pjp);

            // If the method declares an argument of LushContext, inject it (we inject it by copying the values)
            injectLushContext(method, pjp, apiContext);
            // If the method declares an argument of LushTicket, inject it (we inject it by copying the values)
            injectTicket(method, pjp, ticket);

            // Invoke the target method wrapped in a publisher - this allows us to handle exceptions in the Lush way
            if (fluxOnError) {
                return Flux.from((Publisher<?>) pjp.proceed())
                        .onErrorResume(throwable -> {
                            errorHandler(apiContext, throwable);
                            return Flux.empty();
                        })
                        .doOnComplete(() -> {
                            if (log.isDebugEnabled()) log.debug("****");
                        });
            } else {
                return Mono.from((Publisher<?>) pjp.proceed())
                        .onErrorResume(throwable -> {
                            errorHandler(apiContext, throwable);
                            return Mono.empty();
                        })
                        .doOnSuccess(o -> {
                            if (log.isDebugEnabled()) log.debug("****");
                        });
            }
        }

        // Catch all error handler.  Returns an empty Mono or Flux
        catch (final Throwable throwable) {
            throwable.printStackTrace(new StackTraceToLoggerWriter(log));

            LushAdvice advice = apiContext.getAdvice();
            advice.setStatusCode(-99);
            advice.putExtra("lush.isUnexpectedException", true);

            if (log.isDebugEnabled()) log.debug("****");
            return fluxOnError ? Flux.empty() : Mono.empty();
        }
    }
}
