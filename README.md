>La fortuna non esiste: esiste il momento in cui il talento incontra l'occasione.

###### Lucio Anneo Seneca

# SPARQL Event Processing Architecture (SEPA)
SEPA is a publish-subscribe architecture designed to support information level interoperability in smart space applications for the Internet of Things (IoT). The architecture is built on top of a generic SPARQL endpoint where publishers and subscribers use standard SPARQL Updates and Queries. Notifications about events (i.e., **changes in the RDF knowledge base**) are expressed in terms of added and removed SPARQL binding results since the previous notification, limiting the network overhead and facilitating notification processing at subscriber side. 

>The main drawback of Semantic Web technologies concerns the low level of performance that makes it difficult to achieve responsiveness and scalability required in many IoT applications…Semantic Web technologies have been designed to process data sets consisting of big amounts of Resource Description Framework (RDF) triples that evolve constantly but at a much slower rate compared to the rate of elementary events occurring in the physical environment.

###### *A Semantic Publish-Subscribe Architecture for the Internet of Things, Luca Roffia, Francesco Morandi, Jussi Kiljander, Alfredo D’Elia, Fabio Vergari, Fabio Viola, Luciano Bononi, and Tullio Salmon Cinotti, IEEE Internet of Things Journal, DOI: 10.1109/JIOT.2016.2587380)*

The SEPA is framed within W3C Recommendations as shown in the following figure.

![alt text][sepa]

Please refer to [vaimee-documentation](https://github.com/vaimee/sepa-documentation) for a set of **W3C Recommendation drafts** we are writing about the SEPA. The SEPA, as an interoperability platform, aims supporting the development of [Web of Things applications](https://www.w3.org/WoT/).

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

There are several SPARQL endpoint implementations and the number of online SPARQL endopoints is increasing. The SEPA can be locally evaluated using one of them, like [Virtuoso](https://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSSparqlProtocol), [Fuseki](https://jena.apache.org/documentation/serving_data/) or [Blazegraph](https://wiki.blazegraph.com/wiki/index.php/Main_Page) just to name a few. The current implementation has been tested on Blazegraph.

:coffee: [SEPA Engine](build/SEPAengine.jar) :coffee:

## SEPA APIs - including Application Design Pattern (ADP) libraries
If you want to save your time, reuse and share components and contribute to the community, please follow this pattern:

![alt text][pattern]

:star: Start implementing a new **SEPApp** :star:

:coffee: [Java API](build/SEPapi.jar) [Java ADP Library]:coffee:
:snake: [Python](https://github.com/vaimee/sepa-Python3-kpi) :snake:
:iphone: [C](https://github.com/vaimee/sepa-C-kpi) :iphone:
:gem: [Ruby](https://github.com/vaimee/sepaRubyClientLibrary) :gem:
 
 Want more? Contribute! :+1:
 
## SEPA Tools
Let's start with an essential tool: **the SEPA Dashboard** :clap:

:coffee: [Java](build/SEPAdashboard.jar) :coffee:
:icecream: [JavaScript](https://github.com/vaimee/sepa-dashboard) :icecream: 

 Want more? Contribute! :+1:

## Contact info
SEPA stands for *SPARQL Event Processing Architecture* and represent the main research area of the [**Web of Things**](http://wot.arces.unibo.it) working group of [**ARCES**](http://www.arces.unibo.it) (*Advanced Research Center on Electronic Systems*) - [**University of Bologna**](http://www.unibo.it). This repository is maintained by:

Name | Email | On Github
---- | ----- | ---------
Luca Roffia | luca DOT roffia AT unibo DOT it | @lroffia
Fabio Viola | fabio DOT viola AT unibo DOT it | @desmovalvo
Francesco Antoniazzi | francesco DOT antoniazzi AT unibo DOT it | @fr4ncidir

[sepa]: images/sepa.jpg "SPARQL Event Processing Architecture"
[framework]: images/SW_framework.png "SEPA Framework"
[engine]: images/engine.png "SEPA Engine"
[pattern]: images/pattern.jpg "Application Design Pattern"