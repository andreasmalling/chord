# IoT / P2P
- [Milestone 1](#milestone1)
- [Milestone 2](#milestone2)

<a name="milestone1"/>
# Milestone 1

You must implement the basic Chord-ring. Peers should be able to join the ring, and leave the ring in an orderly manner. You should implement the find_successor function, so that you can locate the peer responsible for a given id.

## Requirements

All communication between peers should be RESTful. The individual peer should to a Web browser present a simple page, where the peer’s state (such as id, predecessor, and successor (the latter two presented as links to the respective peers)) can be inspected, and where actions, such as searching for an id, can be performed.

You must document your REST API in a PDF. This PDF must be uploaded to Blackboard, and brought to the milestone meeting.

You may assume that one Chord peer is initially known and available for bootstrapping purposes.

Bonus: Make your system more robust against churn using successor lists.

## Elaboration

There is *no* centralised component in the system — the initial Chord peer is not different in *any way* from the others, it is merely the first to be launched. Similarly, communication between web browser and peer takes place *directly* and not through any central web server. *All* peers act as small web servers.

## Hints

You should make the port of a peer configurable, so that you can run many peers on the same machine. This facilitates debugging, and enables you to launching many peers quickly through a script.

It is *completely* unnecessary for your purpose to use 160 bits long IDs. Use *m* bits, where *m* ≪ 160.

RESTful interfaces can be debugged through browser tools such as [Postman](https://www.getpostman.com/) or command line tools such as [curl](https://curl.haxx.se/).

Any HTTP request [header](https://en.wikipedia.org/wiki/List_of_HTTP_header_fields) features an Accept field, which enables you as developer to ensure that the same request returns, e.g., JSON (when called by a program) or HTML (when called by a browser). Supporting both eases debugging considerably.



<a name="milestone2"/>

# Milestone 2

Having implemented the basic Chord-ring, you should now implement finger tables and use them for more efficient routing, as well as successor lists for increased robustness. The finger tables should be inspectable through your web user interface. Peers should build finger tables and successor lists upon joining Chord, and must maintain them over time.

Test your system with and without finger tables with a non-trivial number of peers (e.g., over 50 peers), and compare the number of routing hops necessary.

Try adding and killing nodes to test whether your Chord network adapts accordingly.

## Requirements

All communication between peers must be RESTful. The individual peer shall to a Web browser present a simple page, where the peer’s state can be inspected, and where actions, such as searching for an id, can be performed.

You must document your REST API in a PDF. This PDF must be uploaded to Blackboard, and brought to the milestone meeting.

You may assume that one Chord peer is initially known and available for bootstrapping purposes.