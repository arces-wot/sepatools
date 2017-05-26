# SPARQL Event Processing Architecture (SEPA)
SEPA is a publish-subscribe architecture designed to support information level interoperability in smart space applications in the Internet of Things (IoT). The architecture is built on top of a generic SPARQL endpoint where publishers and subscribers use standard SPARQL Updates and Queries. Notifications about events (i.e., changes in the RDF knowledge base) are expressed in terms of added and removed SPARQL binding results since the previous notification, limiting the network overhead and facilitating notification processing at subscriber side. The SEPA is framed within W3C Recommendations as shown in the following figure.

![alt text][sepa]

Please refer to [vaimee-documentation](https://github.com/vaimee/sepa-documentation) for a set of W3C Recommendation drafts about the SEPA. The SEPA, as an interoperability platform, aims supporting the development of [Web of Things applications] (https://www.w3.org/WoT/) </h2>

## HOW TO
> Are you in hurry? You do not have time to read the following sections? You cannot wait trying SEPA? 

Here the steps to follow:

1. `git clone https://github.com/vaimee/sepatools.git`
2. Move to the `build` directory of the repository
3. Open a shell (a command prompt) from that directory and type: `java -jar blazegraph.jar`
4. Open a new shell (a command prompt) from the same directory and type: `java -jar SEPAEngine.jar -Dcom.sun.management.config.file=management.properties`
5. The `-Dcom.sun.management.config.file=management.properties` command line argument allows to monitor the SEPA Engine using [JMX](http://www.oracle.com/technetwork/articles/java/javamanagement-140525.html). You can open a new shell (yes, the third one...sorry for that :smile:) and type `jconsole` . Once the console windows is up, select the **Remote Process** checkbox, type **localhost:5555** and use **root** as username and password (this is just a demo ;-))

## SEPA framework
The SEPA software framework is shown in the following figure.  

![alt text][framework]

## SEPA Engine
The SEPA engine is designed to run on top of a [SPARQL 1.1 Processing Service](https://www.w3.org/TR/sparql11-protocol/). There are several implementations SPARQL endpoints and the number of public available ones is increasing. The SEPA can be locally evaluated using one of them, like [Virtuoso](https://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSSparqlProtocol), [Fuseki](https://jena.apache.org/documentation/serving_data/) or [Blazegraph](https://wiki.blazegraph.com/wiki/index.php/Main_Page) just to name a few. The current implementation has been tested on Blazegraph. 

[Out of the shelf SEPA Engine](https://github.com/vaimee/sepatools/tree/master/build (build/SEPAengine.jar)

## SEPA Client APIs
[Out of the shelf JAVA API](https://github.com/vaimee/sepatools/tree/master/build (build/SEPapi.jar)
 
## SEPA Application Design Pattern
[Out of the shelf JAVA Application Design Library](https://github.com/vaimee/sepatools/tree/master/build (build/SEPattern.jar)

## SEPA Tools
[Out of the shelf Java SEPA Dashboard](https://github.com/vaimee/sepatools/tree/master/build (build/SEPAdashboard.jar)

[sepa]: images/sepa.jpg "SPARQL Event Processing Architecture"
[framework]: images/SW_framework.png "SEPA Framework"

