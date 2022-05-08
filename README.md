# ChatFusion

## How to compile

Run Gradle at the project root:
```shell
gradle clean jar
```
The client and server JAR are placed in the 'build' directory.

## Modules

- The 'client' module contains the client code.
- The 'server' module contains the server code.
- The 'common' module contains code used by both the client and server. 
It is a Gradle dependency.
