package com.px3j.lush.web.security;

import com.google.gson.JsonSyntaxException;
import com.px3j.lush.core.ticket.LushTicket;
import com.px3j.lush.core.ticket.TicketUtil;
import com.px3j.lush.web.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Enumeration;

@Component
@Slf4j(topic = "lush.core.debug")
public class LushSecurityContextRepository implements SecurityContextRepository {
    private final TicketUtil ticketUtil;

    @Autowired
    public LushSecurityContextRepository(TicketUtil ticketUtil) {
        this.ticketUtil = ticketUtil;
    }

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        HttpServletRequest request = requestResponseHolder.getRequest();
        Enumeration<String> headers = request.getHeaders(Constants.TICKET_HEADER_NAME);

        // Header isn't available, deny access...
        if (headers == null || !headers.hasMoreElements()) {
            if (log.isDebugEnabled()) {
                log.debug("DENY: Request is missing Lush Ticket header: " + Constants.TICKET_HEADER_NAME);
            }
            return null;
        }

        // Header is available, get the first element.
        final String ticketFromHeader = headers.nextElement();

        try {
            LushTicket ticket = ticketUtil.decrypt(ticketFromHeader);

            TicketAuthenticationToken authToken = new TicketAuthenticationToken(ticket);
            authToken.setAuthenticated(true);
            if (log.isDebugEnabled()) {
                log.debug("ALLOW: userName: " + ticket.getUsername());
            }

            return new SecurityContextImpl(authToken);
        } catch (JsonSyntaxException e) {
            if (log.isDebugEnabled()) {
                log.debug("DENY: Invalid JSON in Lush Ticket header: " + Constants.TICKET_HEADER_NAME);
            }
            return null;
        }
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        // Save context if necessary
        // In this example, we are not saving the context as it's not needed
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        Enumeration<String> headers = request.getHeaders(Constants.TICKET_HEADER_NAME);
        return headers != null && headers.hasMoreElements();
    }

    private SecurityContext fetchSecurityContextAsync(HttpServletRequest request) {
        // Simulate async processing, e.g., fetch from an external service
        // For illustration, returning a new SecurityContextImpl
        return new SecurityContextImpl();
    }
}