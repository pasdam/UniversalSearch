# UniversalSearch
This is a proof of concept of a pluggable Android application. This app allows users to perform searches on different services, that must be installed by the user.

N.B. As a proof of concept, this application has some notable issues, and its use is recommended only for learning purposes.

## Plugins
The plugins can be of two type:
- service: another application that is an Andorid service that can be used from the main application;
- opensearch xml description files: an xml file with the opensearch description of the search engine.
 
Examples are:
- service: https://github.com/pasdam/UniversalSearchPlugin-Wikipedia
- opensearch description file: https://github.com/pasdam/OpenSearchTest/blob/master/descriptions/google.xml
