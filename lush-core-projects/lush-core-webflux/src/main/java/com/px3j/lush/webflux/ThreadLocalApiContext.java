package com.px3j.lush.webflux;

import com.px3j.lush.core.model.LushContext;

/**
 * ThreadLocal to contain ApiContext - allows passing from the WebFlux layer into our Aspects
 *
 * @author Paul Parrone
 */
class ThreadLocalApiContext {
    static ThreadLocal<LushContext> threadLocal = ThreadLocal.withInitial(CarryingContext::new);

    public static LushContext get() {
        return threadLocal.get();
    }
}
