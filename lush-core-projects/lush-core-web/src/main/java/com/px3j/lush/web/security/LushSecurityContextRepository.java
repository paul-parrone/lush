package com.px3j.lush.web.security;

import com.google.gson.JsonSyntaxException;
import com.px3j.lush.core.ticket.LushTicket;
import com.px3j.lush.core.ticket.TicketUtil;
import com.px3j.lush.web.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;

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
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        String ticketFromHeader = request.getHeader(Constants.TICKET_HEADER_NAME);
        if( StringUtils.hasText(ticketFromHeader) ) {
            try {
                LushTicket ticket = ticketUtil.decrypt(ticketFromHeader);

                TicketAuthenticationToken authToken = new TicketAuthenticationToken(ticket);
                authToken.setAuthenticated(true);
                context.setAuthentication(authToken);

                if (log.isDebugEnabled()) {
                    log.debug("ALLOW: userName: " + ticket.getUsername());
                }

                return context;
            }
            catch (JsonSyntaxException e) {
                if (log.isDebugEnabled()) {
                    log.debug("DENY: Invalid JSON in Lush Ticket header: " + Constants.TICKET_HEADER_NAME);
                }
            }
        }
        else {
            if (log.isDebugEnabled()) {
                log.debug("DENY: Request is missing Lush Ticket header: " + Constants.TICKET_HEADER_NAME);
            }
        }

        return context;
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        // Save context if necessary
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        String ticketFromHeader = request.getHeader(Constants.TICKET_HEADER_NAME);
        return StringUtils.hasText(ticketFromHeader);
    }
}