package ${package}.lush.controller;

import com.px3j.lush.core.exception.LushException;
import com.px3j.lush.core.exception.StackTraceToLoggerWriter;
import com.px3j.lush.core.model.AnyModel;
import com.px3j.lush.core.model.LushAdvice;
import com.px3j.lush.core.model.LushContext;
import com.px3j.lush.core.ticket.LushTicket;
import com.px3j.lush.core.ticket.TicketUtil;
import com.px3j.lush.core.util.CryptoHelper;
import com.px3j.lush.web.common.LushControllerMethod;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import ${package}.lush.model.Cat;
import ${package}.lush.feign.ExampleServiceApi;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Example controller that shows how you can take advantage of Lush in your applications endpoints.
 *
 *  @author Paul Parrone
 */
@Slf4j
@RestController
@RequestMapping("/lush/example")
public class ExampleController {
    private TicketUtil ticketUtil;
    private ExampleServiceApi exampleServiceApi;

    @Autowired
    public ExampleController(TicketUtil ticketUtil, ExampleServiceApi exampleServiceApi) {
        this.ticketUtil = ticketUtil;
        this.exampleServiceApi = exampleServiceApi;
    }

    /**
     * Example controller endpoint that will return a new set of crypto keys usable to encrypt/decrypt Lust Ticket(s).
     *
     * @return A map of an access key and a secret key
     */
    @LushControllerMethod
    @GetMapping("crypto-gen")
    @PreAuthorize("isAuthenticated()")
    public Mono<AnyModel> cryptoGen() {
        try {
            log.info( "cryptoGen() has been called" );

            SecretKey secretKey = CryptoHelper.generateKey(256);
            IvParameterSpec ivParameterSpec = CryptoHelper.generateIv();

            String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            String encodedIv  = Base64.getEncoder().encodeToString(ivParameterSpec.getIV());

            return Mono.just(
                    AnyModel.from(
                            "lush.crypto.secret-key", encodedKey,
                            "lush.crypto.access-key", encodedIv
                    )
            );
        }
        catch (NoSuchAlgorithmException e) {
            throw new LushException( e );
        }
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
    public Mono<AnyModel> ping() {
        log.info( "ping() has been called" );
        return Mono.just( AnyModel.from("message","Powered By Lush") );
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
    public Mono<AnyModel> pingUser( @Parameter(hidden = true) LushTicket ticket) {
        log.info( ticket.toString() );
        return Mono.just( AnyModel.from("message", String.format("Powered By Lush - hi: %s", ticket.getUsername())) );
    }

    /**
     * This endpoint illustrates how you can use the included open feign library to call another service.  The traceId
     * is carried across to the calling service.  Also, by providing the Lush Ticket, the log statements in the called
     * service will contain the username.
     *
     * @param ticket The ticket representing the caller of this endpoint.
     * @return An instance of AnyModel as returned by the remote service.
     */
    @LushControllerMethod
    @GetMapping("pingRemote")
    @PreAuthorize("isAuthenticated()")
    public Mono<AnyModel> pingRemote(LushTicket ticket) {
        log.debug( "calling remote" );
        return exampleServiceApi.ping(ticketUtil.encrypt(ticket));
    }

    /**
     * This endpoint illustrates how you can use a Flux to return a collection of data back to the caller.
     *
     * @return A Flux that publishes a list of Cats.
     */
    @LushControllerMethod
    @GetMapping("fluxOfCats")
    @PreAuthorize("isAuthenticated()")
    public Flux<Cat> fluxOfCats() {
        return Flux.fromIterable(
                List.of(
                        new Cat("Gumball", "Tonkinese"),
                        new Cat("Sneeb", "Tonkinese"),
                        new Cat("Hobbes", "Domestic")

                )
        );
    }

    /**
     * This endpoint illustrates how you can use a Flux to return a collection of data back to the caller.
     *
     * @return A Flux that publishes a list of Cats.
     */
    @LushControllerMethod
    @GetMapping("fluxOfCatsWithAdvice")
    @PreAuthorize("isAuthenticated()")
    public Flux<Cat> fluxOfCatsWithAdvice( @Parameter(hidden = true) LushTicket ticket, @Parameter(hidden = true) LushContext lushContext ) {
        //
        // Advice automatically injected by Lush - you can modify it, it will be returned to the caller
        //
        LushAdvice advice = lushContext.getAdvice();

        // You can set a status code for this request - this is different than the HTTP status code, with Lush
        // all requests will return a 200 status code, you use LushAdvice to specify the application level
        // status code,
        advice.setStatusCode( 0 );

        // Advice also lets you set 'extras', this is any number of key/value pairs that is usable to your
        // callers.
        advice.putExtra( "helloMessage", String.format("hello: %s", ticket.getUsername()));
        advice.putExtra( "hasMoreData", false );

        // Warnings are a special category of return type.  You can use these to signify specific things that
        // your caller may need to respond to.  Each LusWarning can have a status code and a set of key/value pairs
        // representing details of the warning.
        advice.addWarning( new LushAdvice.LushWarning(600, Map.of("delayedData", true)));

        // And return data...
        return Flux.fromIterable(
                List.of(
                        new Cat("Gumball", "Tonkinese"),
                        new Cat("Sneeb", "Tonkinese"),
                        new Cat("Hobbes", "Domestic")

                ));
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
    public Mono<AnyModel> endpointHandledException( @Parameter(hidden = true) LushContext lushContext ) {
        // Here, we'll use the LushAdvice to signify the failure to the consumer
        //
        LushAdvice advice = lushContext.getAdvice();

        try {
            methodThatThrowsIOException();
            return Mono.just( AnyModel.from("prop1", "value1") );
        }
        catch (IOException e) {
            // Since we want to handle this ourselves, we do so here

            // First, log it in the Lush style
            e.printStackTrace( new StackTraceToLoggerWriter(log) );

            // Set an application status code in advice:
            advice.setStatusCode( 600 );  // in this scenario the code can be set to something the consumer can understand

            // Possibly add any extras that the consumer can use:
            advice.putExtra( "message", "file not found" );  // again, this can be anything that the consumer can understand

            return Mono.empty();
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
    public Mono<AnyModel> endpointWrappedException( @Parameter(hidden = true) LushContext lushContext ) {
        // Here, we'll use the LushAdvice to signify the failure to the consumer
        //
        LushAdvice advice = lushContext.getAdvice();

        try {
            methodThatThrowsIOException();
            return Mono.just( AnyModel.from("prop1", "value1") );
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
    public Mono<AnyModel> uaeNoLush( @Parameter(hidden = true) LushTicket ticket) {
        // Illustration calling a method that may throw an exception, developers don't need to concern themselves with
        // these as Lush will handle it appropriately.
        methodThatThrowsUnexpectedException();

        return Mono.just( AnyModel.from( "message", String.format( "%s says hi!", ticket.getUsername())) );
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
    public Mono<AnyModel> uae( @Parameter(hidden = true) LushTicket ticket) {
        // Illustration calling a method that may throw an exception, developers don't need to concern themselves with
        // these as Lush will handle it appropriately.
        methodThatThrowsUnexpectedException();

        return Mono.just( AnyModel.from("message", String.format( "%s says hi!", ticket.getUsername())) );
    }

    @LushControllerMethod
    @GetMapping("xray")
    @PreAuthorize("isAuthenticated()")
    public Mono<AnyModel> xray(@Parameter(hidden = true) LushTicket ticket, @Parameter(hidden = true) LushContext context ) {
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

        return Mono.just( AnyModel.from("message", "LushAdvice Attached") );
    }

    private void methodThatThrowsUnexpectedException() {
        throw new LushException( "Illustrate Exception Handling" );
    }
    private void methodThatThrowsIOException() throws   IOException{
        throw new IOException( "This is a illustrative checked exception" );
    }
}
