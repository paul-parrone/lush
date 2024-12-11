package com.px3j.service.cat;

import com.px3j.lush.core.model.LushAdvice;
import com.px3j.lush.core.model.LushContext;
import com.px3j.lush.core.ticket.LushTicket;
import com.px3j.lush.web.common.LushControllerMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/lush/cat")
public class CatController {
    /**
     * This endpoint illustrates how you can use a Flux to return a collection of data back to the caller.
     *
     * @return A Flux that publishes a list of Cats.
     */
    @LushControllerMethod
    @GetMapping("fluxOfCats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Cat>> fluxOfCats() {
        return ResponseEntity.ok(
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
    public ResponseEntity<List<Cat>> fluxOfCatsWithAdvice(LushTicket ticket, LushContext lushContext ) {
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
        return ResponseEntity.ok(
                List.of(
                        new Cat("Gumball", "Tonkinese"),
                        new Cat("Sneeb", "Tonkinese"),
                        new Cat("Hobbes", "Domestic")

                ));
    }

}
