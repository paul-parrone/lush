package com.px3j.lush.webflux;

import brave.baggage.BaggageField;
import com.px3j.lush.core.model.LushContext;
import com.px3j.lush.core.ticket.LushTicket;
import com.px3j.lush.web.common.ControllerDecorator;
import com.px3j.lush.web.common.LushWrappedInvocation;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    BaggageField lushUserNameField;

    @Autowired
    public ReactiveControllerDecorator(BaggageField lushUserNameField, Tracer tracer) {
        super(tracer);
        this.lushUserNameField = lushUserNameField;
    }

    @Around("lushControllerMethods() && execution(public reactor.core.publisher.Mono *..*(..))")
    public Mono<?> monoInvocationAdvice(ProceedingJoinPoint pjp) {
        return Mono.deferContextual(ctx -> {
            LushContext lushContext = ctx.get(LushContext.class.getName());
            if (log.isDebugEnabled()) log.debug("LushContext: " + lushContext);

            return ReactiveSecurityContextHolder.getContext()
                    .doFinally(signal -> {
                        teardownLoggingContext();
                    })
                    .map(sc -> (LushTicket) sc.getAuthentication().getPrincipal())
                    .doOnNext(this::setupLoggingContext)
                    .flatMap((ticket) -> {
//                        monoDecoratorImpl(pjp, lushContext, ticket)
                        LushWrappedInvocation<Mono<?>> lushMethod = LushWrappedInvocation.wrap(pjp, lushContext, ticket);
                        Mono<?> result = lushMethod.invoke();
                        return result != null ? result.onErrorResume(throwable -> Mono.empty()) : Mono.empty(); // Return empty Flux if null
                    });
        });
    }


    @Around("lushControllerMethods() && execution(public reactor.core.publisher.Flux *..*(..))")
    public Flux<?> fluxInvocationAdvice(ProceedingJoinPoint pjp) {
        return Flux.deferContextual(ctx -> {
            LushContext lushContext = ctx.get(LushContext.class.getName());
            if (log.isDebugEnabled()) log.debug("LushContext: " + lushContext);

            return ReactiveSecurityContextHolder.getContext()
                    .doFinally(signal -> teardownLoggingContext())
                    .map(sc -> (LushTicket) sc.getAuthentication().getPrincipal())
                    .doOnNext(this::setupLoggingContext)
//                            fluxDecoratorImpl(pjp, lushContext, ticket)
                    .flatMapMany((ticket) -> {
                                LushWrappedInvocation<Flux<?>> lushMethod = LushWrappedInvocation.wrap(pjp, lushContext, ticket);
                                Flux<?> result = lushMethod.invoke();
                                return result != null ? result.onErrorResume(throwable -> Flux.empty()) : Flux.empty(); // Return empty Flux if null

                    });
        });
    }

    private void setupLoggingContext(LushTicket ticket) {
        if (log.isDebugEnabled()) log.debug("ticket user: " + ticket.getUsername());
        lushUserNameField.updateValue(ticket.getUsername());
        MDC.put("lush-user-name", ticket.getUsername());
    }

    private void teardownLoggingContext() {
        MDC.remove("lush-user-name");
        lushUserNameField.updateValue(null); // Reset the BaggageField to avoid data leakage
    }
}
