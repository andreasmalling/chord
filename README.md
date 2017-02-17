# IoT / P2P
- [Milestone 1](#milestone1)
- [Milestone 2](#milestone2)
- [Milestone 3](#milestone3)
- [Milestone 4](#milestone4)

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



<a name="milestone3"/>

# Milestone 3

Having created the Chord-system, your attention should now turn to the Photon. You should build a setup that reports on a physical phenomenon, e.g., temperature, humidity, light levels. It should be accessible through the Particle.io cloud service.

Extend your Chord peers so that you can register your Photon with your Chord system. Use the id of the Photon to associate the Photon with the matching Chord peer. The state of the Photon shall be visible in the UI of the responsible Chord peer. Thus, it should be possible to search for a particular Photon-id, the responsible peer found, and have its state presented.

You are welcome to add additional data sources to your system.

Bonus: Enable the control of some state on the Photon through the Web UI (on the Chord node responsible for that particular Photon), e.g., turning a LED on/off.

## Requirements

All communication between peers must be RESTful. The individual peer shall to a Web browser present a simple page, where the peer’s state can be inspected, and where actions, such as searching for an id, can be performed.

You must document your REST API in a PDF. This PDF must be uploaded to Blackboard, and brought to the milestone meeting.

You may assume that one Chord peer is initially known and available for bootstrapping purposes.

Bonus: Have your peers check each other for liveness, and update routing information accordingly.

See [RESTful Best Practices](http://www.restapitutorial.com/media/RESTful_Best_Practices-v1_1.pdf) for a guide on how to formulate an API in accordance with the REST principles.



<a name="milestone4"/>

# Milestone 4

With both Chord and Photon in place, it is time to ensure robust persistence of collected data. Your current design has one Chord peer responsible for the Photon's state, but what if that peer is lost? Or, a better matching peer joins the Chord network?

You must extend your Chord peers so that they save the data generated by the Photon they are responsible for. You are welcome to use a third-party library for persistence, e.g., SQLite or CouchDB. Extend your peers, so that all saved data can be displayed at the responsible peer.

Persistence at one peer is not sufficiently safe—extend your peers to replicate data to *k* ( *k* < 1) successors, so that the failure of one peer does not lead to data loss. You should also consider that data collection and replication must continue even if the originally responsible peer has been lost, or a new node is a better match for the Photon.

Bonus: Extend your Chord peer with a visualisation of the collected data, e.g., using [D3](https://d3js.org/).

## Requirements

All communication between peers must be RESTful. The individual peer shall to a Web browser present a simple page, where the peer’s state can be inspected, and where actions, such as searching for an id, can be performed.

You must document your REST API in a PDF. This PDF must be uploaded to Blackboard, and be brought to the milestone meeting.

For this final milestone, you must *also* upload all your code along with instructions on how to get the system running. It would be *very* nice to have a single script that starts the system with a fair number of peers.

You may assume that one Chord peer is initially known and available for bootstrapping purposes.

Bonus: Have your peers check each other for liveness, and update routing information accordingly.

See [RESTful Best Practices](http://www.restapitutorial.com/media/RESTful_Best_Practices-v1_1.pdf) for a guide on how to formulate an API in accordance with the REST principles.