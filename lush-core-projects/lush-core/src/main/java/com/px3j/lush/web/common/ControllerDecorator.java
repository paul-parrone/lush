package com.px3j.lush.web.common;


import com.px3j.lush.core.exception.StackTraceToLoggerWriter;
import com.px3j.lush.core.model.LushAdvice;
import com.px3j.lush.core.model.LushContext;
import com.px3j.lush.core.ticket.LushTicket;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Decorator, applied via AOP, that intercepts calls to any Lush based controllers.  It silently intercepts and provides
 * the Lush functionality.
 *
 * @author Paul Parrone
 */
@Slf4j(topic = "lush.core.debug")
public abstract class ControllerDecorator {
    protected final Tracer tracer;

    @Autowired
    public ControllerDecorator(Tracer tracer) {
        this.tracer = tracer;
    }

    @Pointcut("@annotation(com.px3j.lush.web.common.LushControllerMethod)")
    public void lushControllerMethods() {
    }




}
