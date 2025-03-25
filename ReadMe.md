# Guide

## Setup

Open project in terminal and run: ```docker-compose up```

If everything worked correctly you should have a running neo4j and redis instance running on the standard ports.

Neo4j can be looked at in browser: ```http://localhost:7474/browser/```

Username: neo4j

Password: Reader@123


## Infos

Commons can be ignored.

All endpoints can be accessed through the swagger ui of the gateway application but they may not work directly through it.

The endpoints of the apis can be accessed through the gateway by appending: /api/v1/$service/endpoints

Basic Authentication is used by setting a User-ID header, but you first need to register it.