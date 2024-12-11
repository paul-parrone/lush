package com.px3j.lush.webflux.security;

import com.google.gson.JsonSyntaxException;
import com.px3j.lush.core.ticket.LushTicket;
import com.px3j.lush.core.ticket.TicketUtil;
import com.px3j.lush.web.common.Constants;
import com.px3j.lush.web.security.TicketAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j( topic = "lush.core.debug")
public class LushSecurityContextRepository implements ServerSecurityContextRepository {
    private final TicketUtil ticketUtil;

    @Autowired
    public LushSecurityContextRepository(TicketUtil ticketUtil) {
        this.ticketUtil = ticketUtil;
    }

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }


    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String ticketFromHeader = getTicketFromHeader(exchange);
        if( StringUtils.hasText(ticketFromHeader) ) {
            try {
                LushTicket ticket = ticketUtil.decrypt(ticketFromHeader);

                TicketAuthenticationToken authToken = new TicketAuthenticationToken(ticket);
                authToken.setAuthenticated(true);
                if( log.isDebugEnabled() ) {
                    log.debug( "ALLOW: userName: " + ticket.getUsername() );
                }

                return Mono.just( new SecurityContextImpl(authToken) );
            }
            catch (JsonSyntaxException e) {
                if( log.isDebugEnabled() ) {
                    log.debug( "DENY: Invalid JSON in Lush Ticket header: " + Constants.TICKET_HEADER_NAME );
                }
                return Mono.empty();
            }
        }
        else {
            // Header isn't available, deny access...
            if( log.isDebugEnabled() ) {
                log.debug( "DENY: Request is missing Lush Ticket header: " + Constants.TICKET_HEADER_NAME );
            }
            return Mono.empty();
        }
    }

    private String getTicketFromHeader(ServerWebExchange exchange) {
        List<String> ticketList = exchange.getRequest().getHeaders().get(Constants.TICKET_HEADER_NAME);

        if( ticketList == null || ticketList.isEmpty() ) {
            return null;
        }

        return ticketList.getFirst();
    }
}
