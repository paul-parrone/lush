package com.px3j.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.px3j.service.model.Cat;
import com.px3j.lush.core.model.LushAdvice;
import com.px3j.lush.core.ticket.LushTicket;
import com.px3j.lush.core.ticket.TicketUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.px3j.lush.web.common.Constants.TICKET_HEADER_NAME;

@Slf4j(topic="lush.core.debug")
@ActiveProfiles( profiles = {"clear-ticket"})
@SpringBootTest(classes={LushServiceApp.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LushServiceAppTests {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TicketUtil ticketUtil;

    @Test
    void contextLoads() {
        // empty test that would fail if our Spring configuration does not load correctly
    }

    @Test
    public void testPing()  {
        log.info( "START: testPing" );

        LushTicket ticket = new LushTicket("paul", "", List.of(new SimpleGrantedAuthority("user")));
        final String encodedTicket = ticketUtil.encrypt(ticket);

        HttpHeaders headers = new HttpHeaders();
        headers.set(TICKET_HEADER_NAME, encodedTicket);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/lush/example/ping",
                HttpMethod.GET,
                entity,
                String.class);

        String results = response.getBody();
        log.info("Ping results: {}", results);
        log.info("END: testPing");
    }


    @Test
    public void testPingUser() {
        log.info("START: testPingUser");

        LushTicket ticket = new LushTicket("paul", "", List.of(new SimpleGrantedAuthority("user")));
        final String encodedTicket = ticketUtil.encrypt(ticket);

        HttpHeaders headers = new HttpHeaders();
        headers.set(TICKET_HEADER_NAME, encodedTicket);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/lush/example/pingUser",
                HttpMethod.GET,
                entity,
                String.class
        );

        String results = response.getBody();
        log.info("pingUser results: {}", results);
        log.info("END: testPingUser");
    }

    @Test
    public void testFluxOfCats() {
        log.info("START: testFluxOfCats");

        LushTicket ticket = new LushTicket("paul", "", List.of(new SimpleGrantedAuthority("user")));
        final String encodedTicket = ticketUtil.encrypt(ticket);

        HttpHeaders headers = new HttpHeaders();
        headers.set(TICKET_HEADER_NAME, encodedTicket);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/lush/cat/fluxOfCats",
                HttpMethod.GET,
                entity,
                String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        List<Cat> cats;
        try {
            cats = objectMapper.readValue(responseBody, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("Error parsing response", e);
            return;
        }

        cats.forEach(cat -> log.info(cat.toString()));

        log.info("END: testFluxOfCats");
    }

/*
        @Test
        public void testFluxOfCatsWithAdvice() {
            testFluxOfCatsWithAdviceImpl("tester");
        }
*/

    @Test
    public void testUnexpectedException() {
        log.info("START: testUnexpectedException");

        LushTicket ticket = new LushTicket("paul", "", List.of(new SimpleGrantedAuthority("user")));
        final String encodedTicket = ticketUtil.encrypt(ticket);

        HttpHeaders headers = new HttpHeaders();
        headers.set(TICKET_HEADER_NAME, encodedTicket);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/lush/example/uae",
                HttpMethod.GET,
                entity,
                Map.class
        );

        displayAdvice(response.getHeaders());
        log.info("END: testUnexpectedException");
    }

    @Test
    public void testUnexpectedExceptionNoLush() {
        log.info("START: testUnexpectedExceptionNoLush");

        LushTicket ticket = new LushTicket("paul", "", List.of(new SimpleGrantedAuthority("user")));
        final String encodedTicket = ticketUtil.encrypt(ticket);

        HttpHeaders headers = new HttpHeaders();
        headers.set(TICKET_HEADER_NAME, encodedTicket);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/lush/example/uaeNoLush",
                HttpMethod.GET,
                entity,
                Map.class
        );

        List<String> adviceHeader = response.getHeaders().get("x-lush-advice");
        if (adviceHeader != null && !adviceHeader.isEmpty()) {
            LushAdvice advice = new Gson().fromJson(adviceHeader.getFirst(), LushAdvice.class);
            log.info("Lush LushAdvice: {}", advice.toString());
        }

        log.info("END: testUnexpectedExceptionNoLush");
    }

    @Test
    public void testXray() {
        log.info("START: testXray");
        String username = "paul";

        LushTicket ticket = new LushTicket(username, "", List.of(new SimpleGrantedAuthority("user")));
        final String encodedTicket = ticketUtil.encrypt(ticket);

        HttpHeaders headers = new HttpHeaders();
        headers.set(TICKET_HEADER_NAME, encodedTicket);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/lush/example/xray",
                HttpMethod.GET,
                entity,
                String.class
        );

        displayAdvice(response.getHeaders());
        log.info("Response body is: {}", response.getBody());

        log.info("END: testXray");
    }

        @Test
        public void testWithAdviceThreaded() throws Exception {
            int numThreads = 15;
            try( ExecutorService executor = Executors.newFixedThreadPool( numThreads ); ) {
                for( int i=0; i<numThreads; i++ ) {
                    executor.submit( () -> testFluxOfCatsWithAdviceImpl(UUID.randomUUID().toString()));
                }

                executor.shutdown();
                executor.awaitTermination( 10, TimeUnit.SECONDS );
            }
        }

    private void testFluxOfCatsWithAdviceImpl(String username) {
        username = username == null ? "paul" : username;

        LushTicket ticket = new LushTicket(username, "", List.of(new SimpleGrantedAuthority("user")));
        final String encodedTicket = ticketUtil.encrypt(ticket);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set(TICKET_HEADER_NAME, encodedTicket);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Cat>> response = restTemplate.exchange(
                "/lush/cat/fluxOfCatsWithAdvice",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );

        displayAdvice(response.getHeaders().getFirst("x-lush-advice"));

        Objects.requireNonNull(response.getBody()).forEach(cat -> log.info("{}", cat));
    }

    private void displayAdvice(HttpHeaders headers) {
        List<String> adviceHeader = headers.get("x-lush-advice");
        if (adviceHeader == null || adviceHeader.isEmpty()) {
            log.info("No Lush LushAdvice available");
            return;
        }
        displayAdvice( adviceHeader.getFirst() );
    }

    private void displayAdvice(String adviceJson) {
        log.info( "** START: Lush LushAdvice **" );
        LushAdvice advice = new Gson().fromJson( adviceJson, LushAdvice.class );

        log.info( "**        Status Code: {}", advice.getStatusCode() );
        log.info( "**        Trace Id:    {}", advice.getTraceId() );
        log.info( "**        Extras:" );
        advice.getExtras().forEach( (k,v) -> log.info( "**           extra - key: {} value: {}:", k, v ));
        log.info( "**        Warnings:" );
        advice.getWarnings().forEach( w -> {
            log.info( "**          Code: {}", w.getCode() );
            log.info( "**          Details: " );
            w.getDetail().forEach( (k,v) -> log.info( "**             warning - key: {} details: {}:", k, v ));
            log.info( "**          " );
        });

        log.info( "** END:   Lush LushAdvice **" );
    }
}
