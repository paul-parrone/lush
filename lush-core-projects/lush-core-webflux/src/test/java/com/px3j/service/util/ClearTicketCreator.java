package com.px3j.service.util;

import com.google.gson.Gson;
import com.px3j.lush.core.ticket.LushTicket;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

public class ClearTicketCreator {
    public static void main(String[] args) {
        String userName = "lush";

        if( args.length > 0 ) {
            userName = args[0];
        }

        List<SimpleGrantedAuthority> roles = List.of(new SimpleGrantedAuthority("user"));

        if( args.length > 1 ) {
            roles = new ArrayList();
            String[] rolesArg = args[1].split(",");
            for (String r : rolesArg) {
                roles.add(new SimpleGrantedAuthority(r));
            }
        }

        final LushTicket ticket = new LushTicket("lush", "", roles);
        String jsonTicket = new Gson().toJson(ticket);

        System.out.println( "Clear ticket is below:" );
        System.out.println(jsonTicket);
    }

}
