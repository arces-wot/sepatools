# SPARQL Event Processing Architecture (SEPA)
SEPA is a publish-subscribe architecture designed to support information level interoperability in smart space applications in the Internet of Things (IoT). The architecture is built on top of a generic SPARQL endpoint where publishers and subscribers use standard SPARQL Updates and Queries. Notifications about events (i.e., changes in the RDF knowledge base) are expressed in terms of added and removed SPARQL binding results since the previous notification, limiting the network overhead and facilitating notification processing at subscriber side. 

>The main drawback of Semantic Web technologies concerns the low level of performance that makes it difficult to achieve responsiveness and scalability required in many IoT applications…Semantic Web technologies have been designed to process data sets consisting of big amounts of Resource Description Framework (RDF) triples that evolve constantly but at a much slower rate compared to the rate of elementary events occurring in the physical environment.

*A Semantic Publish-Subscribe Architecture for the Internet of Things, Luca Roffia, Francesco Morandi, Jussi Kiljander, Alfredo D’Elia, Fabio Vergari, Fabio Viola, Luciano Bononi, and Tullio Salmon Cinotti, IEEE Internet of Things Journal, DOI: 10.1109/JIOT.2016.2587380)*

The SEPA is framed within W3C Recommendations as shown in the following figure.

![alt text][sepa]

Please refer to [vaimee-documentation](https://github.com/vaimee/sepa-documentation) for a set of W3C Recommendation drafts about the SEPA. The SEPA, as an interoperability platform, aims supporting the development of [Web of Things applications] (https://www.w3.org/WoT/) </h2>

## HOW TO
> Are you in hurry? You do not have time to read the following sections? You cannot wait trying SEPA? :smile:

Here the steps to follow:

1. `git clone https://github.com/vaimee/sepatools.git`
2. Move to the `build` directory of the repository
3. Open a shell (a command prompt) from that directory and type: `java -jar blazegraph.jar`
4. Open a new shell (a command prompt) from the same directory and type: `java -jar SEPAEngine.jar -Dcom.sun.management.config.file=management.properties`
5. The `-Dcom.sun.management.config.file=management.properties` command line argument allows to monitor the SEPA Engine using [JMX](http://www.oracle.com/technetwork/articles/java/javamanagement-140525.html). You can open a new shell (yes, the third one...sorry for that :smile:) and type `jconsole` . Once the console windows is up, select the **Remote Process** checkbox, type **localhost:5555** and use **root** as username and password (this is just a demo :bowtie:). Now you can see (and change) some engine parameters.

## SEPA framework
The SEPA software framework is shown in the following figure.  

![alt text][framework]

## SEPA Engine
The SEPA engine is designed to run on top of a [SPARQL 1.1 Processing Service](https://www.w3.org/TR/sparql11-protocol/).

![alt text][engine]

There are several implementations SPARQL endpoints and the number of public available ones is increasing. The SEPA can be locally evaluated using one of them, like [Virtuoso](https://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSSparqlProtocol), [Fuseki](https://jena.apache.org/documentation/serving_data/) or [Blazegraph](https://wiki.blazegraph.com/wiki/index.php/Main_Page) just to name a few. The current implementation has been tested on Blazegraph. 

:coffee: [SEPA Engine](https://github.com/vaimee/sepatools/tree/master/build (build/SEPAengine.jar)

## SEPA Client APIs
:coffee: [Java API](https://github.com/vaimee/sepatools/tree/master/build (build/SEPapi.jar)
 
## SEPA Application Design Pattern
If you to save you time, reuse your components and contribute to the community, please follow this pattern:

![alt text][pattern]

:star: Start implementing a new **SEPApp** :star:

:coffee: [Java Application Design Library](https://github.com/vaimee/sepatools/tree/master/build (build/SEPattern.jar)

## SEPA Tools
:coffee: [Java SEPA Dashboard](https://github.com/vaimee/sepatools/tree/master/build (build/SEPAdashboard.jar)

[sepa]: images/sepa.jpg "SPARQL Event Processing Architecture"
[framework]: images/SW_framework.png "SEPA Framework"
[engine]: images/engine.png "SEPA Engine"
[pattern]: images/pattern.jpg "Application Design Pattern"