# sps Simple Pub Sub

How does it work?
- Subscribe to events by id
- Publish events using an id and key-value as data
- Events with no subs are automatically sent to a schema generator. 
Schemagen will thereby contain an inventory of all events in use.

- Schema updates
- When creating an instance of the publisher, a newer schema can be supplied and the schema gen
will be updated with the new details.

The purpose of the schema is to have a single source of truth on what events exists, 
and possibly can be extended in the future to support autodetect of dead events etc.

### The protocol:

the protocol is pluggable. Built in support will exist for http/rest and shared database 
(postgres). Best performance is when both are available as protocols for sps. It can then
fall back between the protocols, and pick and choose depending on what is best suited.

### Subscribing

- A client subscribes by providing information about itself the Subscriber
- Including URL on where the publisher is supposed to send the events.
- A subscriber should almost always be e.g. a DNS name representing a whole system, 
- not the instances. 
- When subscribing to an event, a schema can be supplied, allowing keys to be renamed in the event
This means that if the event has "name" as a field, the subscription can supply name=givenName
That means that the event will automatically be sent on the expected format. Making it easier
to transition to newer versions of the same event.


### sps-inlet
To be able to actually receive the events, the inlet needs to be embedded / used by the
http server. The gist of it: 
- Create an endpoint that can receive a post
- Map the json to collection of SpsEvents
- Supply this list to the inlet, and it will take care of the rest
- Map the response from the inlet as http response for ack/nack purposes

Here a fair bit of custom pluming is needed, an example using javalin exists in the samples.
Sps aims to support any flavor of http server, and a simple endpoint should not be that much work

### Sending events
Create an instance of the Publisher and simply publish SpsEvents. It will pickup subs from the
database and send the events over.

### Planned features / existing features
- Fully exchangeable between http and sql as protocol
- Retrying configuration
- Circuit breaker support. Configure how many nacks should result in the publisher not even
sending the event to the subscriber with a flipped circuit. 

Automatically rediscover if the communication works again by sending just a few events over
Persist events instead of sending them to preserve the data
- Require a resub of events. Subscribers that have not resubbed to an event in x time
will start to fail the built in health check. This is force an updated view on what subs exist
- Handling versions is done by providing a translation schema from the subscriber side as explained before
- Health check will compare when new properties got added with when the last subscription was done
- 

