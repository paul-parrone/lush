package com.px3j.lush.web.common;

import com.px3j.lush.core.exception.LushException;
import com.px3j.lush.core.exception.StackTraceToLoggerWriter;
import com.px3j.lush.core.model.LushAdvice;
import com.px3j.lush.core.model.LushContext;
import com.px3j.lush.core.ticket.LushTicket;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Optional;

@Slf4j(topic = "lush.core.debug")
public class LushWrappedInvocation<T> {
    private final ProceedingJoinPoint pjp;
    private final LushContext context;
    private final LushTicket ticket;

    public static <T> LushWrappedInvocation<T> wrap(ProceedingJoinPoint pjp, LushContext context, LushTicket ticket) {
        return new LushWrappedInvocation<>(pjp, context, ticket);
    }

    public T invoke()  {
        try {
            if (log.isDebugEnabled()) {
                log.debug("****");
                log.debug("lush wrapped");
            }

            // Get the target method from the join point, use this to inject parameters.
            Method method = getMethodBeingCalled(pjp);
            // If the method declares an argument of LushContext, inject it (we inject it by copying the values)
            injectLushContext(method, pjp, context);
            // If the method declares an argument of LushTicket, inject it (we inject it by copying the values)
            injectTicket(method, pjp, ticket);

            return (T) pjp.proceed();
        }
        catch (Throwable throwable) {
            handleError(context, throwable);
            return null;
//            throw throwable;
        }
        finally {
            if (log.isDebugEnabled()) log.debug("****");
        }
    }

    private LushWrappedInvocation(ProceedingJoinPoint pjp, LushContext context, LushTicket ticket) {
        this.pjp = pjp;
        this.context = context;
        this.ticket = ticket;
    }


    /**
     * Helper method to inject the LushTicket if the method being called requests it.
     *
     * @param method The method being called.
     * @param pjp    The joinpoint.
     * @param ticket The ticket instance to inject.
     */
    private void injectTicket(Method method, ProceedingJoinPoint pjp, LushTicket ticket) {
        findArgumentIndex(method, LushTicket.class)
                .ifPresent((i) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Lush - Injecting LushTicket");
                    }

                    LushTicket contextArg = (LushTicket) pjp.getArgs()[i];
                    contextArg.populateFrom(ticket);
                });
    }

    /**
     * Helper method to inject the LushContext if the method being called requests it.
     *
     * @param method      The method being called
     * @param pjp         The joinpoint.
     * @param lushContext The context instance to inject.
     */
    private void injectLushContext(Method method, ProceedingJoinPoint pjp, LushContext lushContext) {
        findArgumentIndex(method, LushContext.class)
                .ifPresent((i) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Lush - Injecting LushContext");
                    }

                    LushContext contextArg = (LushContext) pjp.getArgs()[i];
                    contextArg.setTraceId(lushContext.getTraceId());
                    contextArg.setAdvice(lushContext.getAdvice());
                });
    }

    /**
     * Find the argument index of the parameter of type clazz (if there is one) and return the index.
     *
     * @param method The method object to check.
     * @param clazz  The type of argument to find.
     * @return The index if found, or empty.
     */
    private Optional<Integer> findArgumentIndex(Method method, Class clazz) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] == clazz) {
                return Optional.of(i);
            }
        }

        return Optional.empty();
    }

    /**
     * Extract a Method object representing the method being called from the joinpoint.
     *
     * @param pjp The joinpoint to inspect.
     * @return A Method instance represent the method being called.
     * @throws NoSuchMethodException If the method being called cannot be found.
     * @throws SecurityException     If the method cannot be accessed.
     */
    private Method getMethodBeingCalled(ProceedingJoinPoint pjp) throws NoSuchMethodException, SecurityException {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        if (method.getDeclaringClass().isInterface()) {
            method = pjp.getTarget().getClass().getDeclaredMethod(signature.getName(), method.getParameterTypes());
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Lush ControllerDecorator - invoking: %s::%s", method.getDeclaringClass(), method.getName()));
        }

        return method;
    }


    /**
     * Helper method to populate the returned LushAdvice properly in the event that an unexpected exception occurs during
     * this call.
     *
     * @param lushContext The context to populate.
     * @param throwable   The exception causing the error.
     */
    private void handleError(LushContext lushContext, Throwable throwable) {
        throwable.printStackTrace(new StackTraceToLoggerWriter(log));

        LushAdvice advice = lushContext.getAdvice();
        if (advice == null) {
            log.warn("LushAdvice is null within LushContext - cannot set status codes");
            return;
        }

        advice.setStatusCode(-99);
        advice.putExtra("lush.isUnexpectedException", true);
    }

}
