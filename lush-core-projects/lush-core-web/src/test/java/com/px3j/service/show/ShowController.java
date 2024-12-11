package com.px3j.service.show;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.px3j.service.show.impl.FeignRemoteServiceImpl;
import com.px3j.service.show.impl.RestRemoteServiceImpl;
import com.px3j.lush.core.exception.LushException;
import com.px3j.lush.core.exception.StackTraceToLoggerWriter;
import com.px3j.lush.core.model.AnyModel;
import com.px3j.lush.core.model.LushAdvice;
import com.px3j.lush.core.model.LushContext;
import com.px3j.lush.core.ticket.LushTicket;
import com.px3j.lush.web.common.LushControllerMethod;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/**
 * Example controller that shows how you can take advantage of Lush in your applications endpoints.
 *
 *  @author Paul Parrone
 */
@Slf4j
@RestController
@RequestMapping("/lush/show")
public class ShowController {
    private final FeignRemoteServiceImpl feignRemoteService;
    private final RestRemoteServiceImpl restRemoteService;
    private final Tracer tracer;

    public ShowController(FeignRemoteServiceImpl feignRemoteService, RestRemoteServiceImpl restRemoteService, Tracer tracer) {
        this.feignRemoteService = feignRemoteService;
        this.restRemoteService = restRemoteService;
        this.tracer = tracer;
    }

    /**
     * Example controller endpoint that returns a String (wrapped by a Mono) as we are using Spring WebFlux.
     *
     * A few things to note:
     * <ul>
     *     <li>@PreAuthorize is automatically wired to recognize a LushTicket as authenticated.</li>
     *     <li>This controller doesn't use the LushTicket so it isn't a parameter, see below for how to have ticket injected.</li>
     * </ul>
     *
     * @return A Mono wrapping a hard-coded String
     */
    @LushControllerMethod
    @GetMapping("ping")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnyModel> ping() {
        log.info( "ping() has been called" );
        return ResponseEntity.ok( AnyModel.from("message","Powered By Lush") );
    }

    /**
     * This endpoint illustrates how to use the ticket in a controller endpoint.  You simply declare it as a parameter
     * and Lush will automatically inject it.
     *
     * @param ticket The ticket representing the user triggering this request.
     * @return A Mono with a String containing the username from the LushTicket
     */
    @LushControllerMethod
    @GetMapping("pingUser")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnyModel> pingUser( LushTicket ticket) {
        log.info( ticket.toString() );
        return ResponseEntity.ok( AnyModel.from("message", String.format("Powered By Lush - hi: %s", ticket.getUsername())) );
    }

    @LushControllerMethod
    @GetMapping("pingRemote")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnyModel> pingRemote( LushTicket ticket) {
        log.info( ticket.toString() );
        String ticketJson = null;

        try {
            ticketJson = new ObjectMapper().writeValueAsString( ticket );
        } catch (JsonProcessingException e) {
            ticketJson = "";
        }

        return ResponseEntity.ok( feignRemoteService.ping( ticketJson ) );
    }

    @LushControllerMethod
    @GetMapping("pingRemoteRt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnyModel> pingRemoteRt( LushTicket ticket) {
        log.info( ticket.toString() );
        String ticketJson = null;

        try {
            log.info( "Scope:" + this.tracer.getAllBaggage() );
            ticketJson = new ObjectMapper().writeValueAsString( ticket );
            return ResponseEntity.ok( restRemoteService.ping( ticketJson ) );
        }
        catch (JsonProcessingException e) {
            return ResponseEntity.ok( AnyModel.from("message", "couldn't ping remote") );
        }

    }

    /**
     * This endpoint illustrates how an endpoint may handle checked exceptions on its own.
     *
     * @param lushContext  The LushContext for this request.
     * @return A simple key/value pair.
     */
    @LushControllerMethod
    @GetMapping("endpointHandledException")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnyModel> endpointHandledException( LushContext lushContext ) {
        // Here, we'll use the LushAdvice to signify the failure to the consumer
        //
        LushAdvice advice = lushContext.getAdvice();

        try {
            methodThatThrowsIOException();
            return ResponseEntity.ok( AnyModel.from("prop1", "value1") );
        }
        catch (IOException e) {
            // Since we want to handle this ourselves, we do so here

            // First, log it in the Lush style
            e.printStackTrace( new StackTraceToLoggerWriter(log) );

            // Set an application status code in advice:
            advice.setStatusCode( 600 );  // in this scenario the code can be set to something the consumer can understand

            // Possibly add any extras that the consumer can use:
            advice.putExtra( "message", "file not found" );  // again, this can be anything that the consumer can understand

            return ResponseEntity.ok( AnyModel.from("prop1", "value1") );
        }
    }

    /**
     * This endpoint illustrates how an endpoint may handle checked exceptions on its own.
     *
     * @param lushContext  The LushContext for this request.
     * @return A simple key/value pair.
     */
    @LushControllerMethod
    @GetMapping("endpointWrappedException")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnyModel> endpointWrappedException( LushContext lushContext ) {
        // Here, we'll use the LushAdvice to signify the failure to the consumer
        //
        LushAdvice advice = lushContext.getAdvice();

        try {
            methodThatThrowsIOException();
            return ResponseEntity.ok( AnyModel.from("prop1", "value1") );
        }
        catch (IOException e) {
            // Our internal API throws a checked exception, Lush believes only in unchecked exceptions.  Endpoint can
            // simply wrap it in a LushException and rethrow it.  The architecture will kick in and ensure that it is
            // logged/propagated to the caller properly.
            throw new LushException( "couldn't open that file", e );
        }
    }


    /**
     * This endpoint illustrates an unexpected exception that isn't wrapped by Lush. (notice, no @LushControllerMethod
     * annotation)
     *
     * @return A String containing a message to the caller.
     */
    @GetMapping("uaeNoLush")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnyModel> uaeNoLush( LushTicket ticket) {
        // Illustration calling a method that may throw an exception, developers don't need to concern themselves with
        // these as Lush will handle it appropriately.
        methodThatThrowsUnexpectedException();

        return ResponseEntity.ok( AnyModel.from( "message", String.format( "%s says hi!", ticket.getUsername())) );
    }

    /**
     * This endpoint illustrates how Lush will wrap controller methods and provide consistent exception handling
     * and logging.
     *
     * @return A String containing a message to the caller.
     */
    @LushControllerMethod
    @GetMapping("uae")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnyModel> uae( LushTicket ticket) {
        // Illustration calling a method that may throw an exception, developers don't need to concern themselves with
        // these as Lush will handle it appropriately.
        methodThatThrowsUnexpectedException();

        return ResponseEntity.ok( AnyModel.from("message", String.format( "%s says hi!", ticket.getUsername())) );
    }

    @LushControllerMethod
    @GetMapping("xray")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnyModel> xray(LushTicket ticket, LushContext context ) {
        LushAdvice advice = context.getAdvice();

        log.info( "Injected ticket: {}", ticket.toString() );
        log.info( "Injected context: {}", context);

        /* You can set a status code in advice - this is not the same as the HTTP Status Code */
        advice.setStatusCode( 555 );

        /* You can add extra information to return via LushAdvice */
        advice.putExtra( "recommendation", "use Lush" );

        /* You can add warnings to advice */
        advice.addWarning( new LushAdvice.LushWarning(1, Map.of( "collision", "field1,field2")));
        advice.addWarning( new LushAdvice.LushWarning(1, Map.of( "count", 100)));

        return ResponseEntity.ok( AnyModel.from("message", "LushAdvice Attached") );
    }

    private void methodThatThrowsUnexpectedException() {
        throw new LushException( "Illustrate Exception Handling" );
    }
    private void methodThatThrowsIOException() throws   IOException{
        throw new IOException( "This is a illustrative checked exception" );
    }
}
