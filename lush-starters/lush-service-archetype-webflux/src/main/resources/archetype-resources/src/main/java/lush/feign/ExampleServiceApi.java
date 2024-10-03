package ${package}.lush.feign;

import com.px3j.lush.core.model.AnyModel;
import feign.Headers;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Mono;

@ReactiveFeignClient(name="CHANGE-TO-REMOTE-SERVICE-NAME")
@Headers({ "Accept: application/json" })
public interface ExampleServiceApi {
    @RequestMapping(method = RequestMethod.GET, value = "/lush/example/ping")
    Mono<AnyModel> ping(@RequestHeader("x-lush-ticket") String ticket);
}
