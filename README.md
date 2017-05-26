# SPARQL Event Processing Architecture
The proposed architecture implementing a content based publish-subscribe mechanism over SPARQL is named SEPA (SPARQL Event Processing Architecture). The core component of SEPA is the SPARQL SE Protocol Service (also know as: SEPA Engine). The SEPA Engine implements the subscription mechanisms and algorithms. The SEPA is intended to be used in dynamic contexts where detecting events is critical. In such contexts, the use of SPARQL queries MAY be inefficient and MAY not guarantee to detect all the events because of their asynchronous nature. The SEPA is framed within W3C Recommendations as shown in the following figure.

![alt text][logo]

[logo]: https://github.com/vaimee/sepatools/blob/master/images/sepa.jpg "SPARQL Event Processing Architecture"