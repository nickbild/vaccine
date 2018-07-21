# Vaccine


Vaccine is an early software threat detection system inspired by the adaptive immune system and plant communication. It is designed to work in concert with existing antivirus software. Vaccine is a first line of defense -- it senses new threats as they present themselves in the wild, and provides the information needed to quickly "vaccinate" antivirus software to protect against new threats before they spread.

## Compilation

Place *.java files in a new directory, then run the command:

javac *.java

NOTE: org.sqlite.JDBC is required:

https://bitbucket.org/xerial/sqlite-jdbc

## Server

To start the server, run:

java VaccineServer <port>

For example, to start listening on port 8081, run:

java VaccineServer 8081

Server data is stored in the SQLite3 database named "vaccine_server.db".

## Client

To hash executables and add them to the local database, run:

java VaccineClient runHash <directory>

To transmit hashes to the server, run:

java VaccineClient transmitHash <server host> <server port>

To alert the server of a panic condition, run:

java VaccineClient panic <server host> <server port>

Client data is stored in the SQLite3 database named "vaccine.db".

##Analysis

To run analytics, run:

java VaccineAnalysis overrep

This must be done from the machine with the server database installed.

## Further Information

https://nickbild79.firebaseapp.com/#!/vaccine

