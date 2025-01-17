Lush strives to have a very small learning curve by building on the same patterns that Spring already provides - if you know Spring, you know Lush.  As you'll see in the examples below, Lush specific behavior is completely optional and provided via simple parameter injection and/or AOP.

By providing a small but powerful set of core classes and defining a set of simple protocols Lush helps you to create an application that is production ready and architecturally sound.  All achieved without developers being burdened with the details on how to make these things happen.




####Things to know:


Controller

Automatic injection of Context if necessary





####Request Categories
* **Non-conversational** - the caller requests some operation, the operation either succeeds or it fails unexpectedly the service will not knowingly refuse to perform the operation.

* **Conversational** - the caller requests some operation, the service may refuse to perform the action due to some validation or other state of the system

For the non-converational type requests, you can envision method signatures as shown below - bear in mind that any operation can trigger an unexpected exception, this is always signaled via runtime exceptions hence you won't see it in the mehod signature.


Retrieve - simply meant to return 1..n of some entity
```java
    @RequestMapping(value = "findOne", method = RequestMethod.POST)
    Flux<Cat> fetchAll() {}
```

Create/Update : performs some update in the system - no validation of data required.
```java
    @RequestMapping(value = "updateAge", method = RequestMethod.POST)
    Mono<Cat> updateAge( @RequestParam int age )
```


The signatures above don't need to signal anything specific to the caller - it either returns the requested data or fails unexpectedly causing a RuntimeException (as you'll see later this can be magically handled by the front-end portion of the Lush Framework).

But, what do we do when the service has to convey to the client that it refused to perform the operation and why it has done so?

The one obvious way is to update the method signature to return some 'wrapper object' that carries both bits of information.

For example:
```java
    class MaybeACatButPossiblyAMessage {
        Cat mayBePopulatedCat;
        RandomInfoAsToWhyOperationWasRefused refusalDetails;
    };

    @RequestMapping(value = "updateAge", method = RequestMethod.POST)
    Mono<MaybeACatButPossiblyAMessage> updateAge( @RequestParam int age )
```

The mechanism above works however it puts a lot of burden on both the caller and the callee (TODO: details on this).  The Lush Framework takes a different approach that reduces the burden on the developer, results in cleaner method calls and allows for a 'conversational approach'.

If the developer of a service endpoint determines that there is a reason that it may refuse the operation they can signal to the Framework that the method is indeed conversational.  How does it do this?

Let's look at an example:
```java
    @RequestMapping(value = "updateAge", method = RequestMethod.POST)
    Mono<Cat> updateAge( @RequestParam int age, Context lushContext )
```

A couple of things to notice:

1. The return type hasn't changed
2. There is an extra object passed to the method - Context


This allows the framework to evolve by adding information to the Context class as needed.






Create/Update : performs some update in the system - validation of data required (action may or may not be taken based on input).


In all cases there are 1 or more outcomes.

Retrieve:
Success
Unexpected exception











#### NOTES BELOW, NOT COMPLETED YET...
* Log tracing
* Automatic Exception handling
* Exception propagation
* Security



#### Environment Variables
CONSUL_HOST
CONSUL_PORT

#### Built-in profiles
developer


### TODO: Things to explain
* Ticket
* Security
* Advice/Warnings
* LushContext
*


Lush tries to break things down into three types of endpoint requests:

1. Just give me some list of data.
2. Submit some data (probably from a form) and update some persistent store.
3. Submit some data, validate it, let me know if the data is accepted or not.

On the surface, this is simple and in the happy path it is.  One can use the normal Spring abstractions to create a Controller, Service and Dao and all is well.  But, if we are thinking of making sure this is production ready, a bunch of other ugly questions rear their heads:

1. What if an exception occurs?
2. What if I'm validating the input and something is missing/incorrect?
3. If something unexpected does go wrong, how will we troubleshoot it?

Now, we can leave the answers to these questions up to each individual developer but that is guaranteed to result in very inconsistent results.  This is the space that Lush tries to fill in.

How does Lush do this?

* Enhanced controller implementation patterns - Spring with extras.
* A simple and well-defined conversation protocol.
* *Invisible* exception handling - through the power of AOP.
* Integrated front-end libraries - automatically carry Lush patterns to the front-end.
* Automatic injection of LushContext and/or Passport if needed.
* Automatic propagation of requesting user across Lush based services.
* Automatic propagation of trace id across Lush based services.
* Automatically secure services - via a Lush Ticket.
* Special 'Developer Mode' to make it less burdensome to run services on developer machines.
*




###Requests
The three types of requests:

* Just give me some data
* Submit some data (aka: form)
* Conversational - submit some data, may result is acceptance or denial based on some validation


**Just give me some data**
**Submit some data**
Lush stays out of the way, you can build your controller exactly as you would in Spring.  No extra work is *necessary* but Lush is still there to handle any unexpected conditions in the Lush way.

TODO: Example



**Conversational**
Again, Lush stays out of the way, but through the Lush context, the server side code can send result details back to the caller.  In this manner, Lush allows for data to be sent back as well as 'advice' as to why or why not the operation was allowed to happen.

TODO: Example





###Integrations

####Logstash JSON

*Make sure Docker VM has 4gb (Docker Desktop Settings)*

```shell
docker network create lush-dev

docker pull logstash:8.0.0
docker run -d --name logstash --net lush-dev -p 5601:5601 logstash:8.0.0

docker run --name logstash --net lush-dev --rm -it -v ~/docker/logstash/pipeline:/usr/share/logstash/pipeline/ -v ~/docker/logstash/logs:/usr/share/logstash/logs/ docker.elastic.co/logstash/logstash:8.0.0

docker pull kibana:8.0.0
docker run -d --name kibana --net lush-dev -p 5601:5601 kibana:8.0.0

docker pull elasticsearch:8.0.0
docker run -d --name elasticsearch --net lush-dev -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:8.0.0
```

[Kibana](http://localhost:5601)
[Spring Boot Admin](http://localhost:9090/wallboard)




#### The "developer" Profile


#### Gateway notes
Default, uses firebase
Out of box ENV

if the insecure spring profile is active then gateway only accepts the x-lush-who header
set to the value of an encoded ticket - this is useful for local development and testing
purposes



GOOGLE_APPLICATION_CREDENTIALS - points to the firebase configuration file

CONSUL_HOST - if you want to override host/port of consul, only applies in developer profile

LUSH_PASSPORT_LOCATION - the location of the ticket service if you choose to use the Lush gateway
out of the box





Lush Image:
http://cliparts.co/clipart/2398680


