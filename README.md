# The Winning Code for the UofA Pason Coding Competition 2016

## Strategy

Team Ascension strategy:
- fire at closest target that is within range, not obstructed by a wall, and not in the path of a friendly tank
- dodge incoming projectiles by spinning and moving away
- A* pathfinding with jump point search optimization

## Dependencies

### 1. ZeroMQ

[ZeroMQ](http://www.zeromq.org/)

### 2. ZeroMQ Java Binding

[ZeroMQ Java Binding](http://www.zeromq.org/bindings:java)

## Compiling the Client (from the same directory as this file)

### Ubuntu

javac -cp /usr/local/share/java/zmq.jar:. tankbattle/client/stub/Client.java

### Windows

javac -cp .;C:\zmq\java\lib\zmq.jar tankbattle\client\stub\Client.java

## Running the Client (from the same directory as this file)

### Ubuntu  

java -Djava.library.path=/usr/local/lib -cp /usr/local/share/java/zmq.jar:. tankbattle.client.stub.Client <ip-address> <team-name> <password> <match-token>

### Windows

java -Djava.library.path=C:\zmq\java\lib -cp .;C:\zmq\java\lib\zmq.jar tankbattle.client.stub.Client <ip-address> <team-name> <password> <match-token>


