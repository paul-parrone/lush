package com.px3j.lush.webflux;

import com.google.gson.Gson;
import com.px3j.lush.core.model.LushAdvice;
import com.px3j.lush.core.model.LushContext;
import com.px3j.lush.web.common.Constants;
//import io.micrometer.tracing.Span;
//import io.micrometer.tracing.Tracer;
//import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFilter that applies Lush behaviors to a request/response.
 *
 * @author Paul Parrone
 */
@Slf4j
@Component
public class ReactiveEndpointFilter implements WebFilter {
    private final Tracer tracer;
    private final ContextSnapshotFactory contextSnapshotFactory = ContextSnapshotFactory.builder().build();

    @Autowired
    public ReactiveEndpointFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, WebFilterChain webFilterChain) {
        return webFilterChain.filter(exchange)
                // Add a LushAdvice instance to the publisher context so that it can be used by the decorator
                //
                .contextWrite(ctx -> {
                    try (ContextSnapshot.Scope scope = this.contextSnapshotFactory.setThreadLocalsFrom(ctx,
                            ObservationThreadLocalAccessor.KEY)) {

                        final String requestKey = generateTraceId(tracer);
                        final LushAdvice advice = new LushAdvice(requestKey, 200);

                        LushContext lushContext = new LushContext();
                        lushContext.setTraceId(requestKey);
                        lushContext.setAdvice(advice);

                        // Set up the exchange to add the Lush advice response header once the controller has done it's work.
                        exchange.getResponse().beforeCommit(() -> Mono.deferContextual(Mono::just).doOnNext(ctx2 -> {
                            HttpHeaders headers = exchange.getResponse().getHeaders();
                            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, Constants.ADVICE_HEADER_NAME);
                            headers.add(Constants.ADVICE_HEADER_NAME, new Gson().toJson(lushContext.getAdvice()));
                        }).then());

                        // return the updated context
                        return ctx.put(LushContext.class.getName(), lushContext);
                    }
                });
    }

    /**
     * Generate a request key that matches the current span of the Tracer
     *
     * @return A String containing the request key.
     */
    private String generateTraceId(Tracer tracer) {
        String contextKey = "?/?";

        Span currentSpan = tracer.currentSpan();
        if( currentSpan != null ) {
            contextKey = currentSpan.context().traceId() + "," + currentSpan.context().spanId();
        }

        return contextKey;
    }
}
