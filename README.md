Sailfish-core
==========

This is the official Sailfish-core project repository.

## intro

Sailfish is a test automation tool whose primary purpose is testing of bi-directional message flows in distributed trading platforms and market data delivery systems. It is a purely back-end tool (meaning that it does not have any front-end GUI testing capabilities) that is typically connected to message gateways / APIs utilized by trading or market data traffic.

The purpose of Sailfish is to minimize manual intervention required to execute test suites. In its more sophisticated deployments Sailfish makes it possible to achieve fully autonomous scheduled test execution that does not require ongoing operator monitoring.

Sailfish has a modular structure whereby a shared framework is used in conjunction with specialized plug-ins. Separate plug-ins are used for each protocol version, such as industry-standard protocols (e. g. FIX, FAST, NTG, ITCH) and proprietary protocols.


## build and run instructions

Build and publish JavaScript Matrix Editor to the local repository (shared folder in the repository root).
Execute command from the FrontEnd/JSMatrixEditor folder
```
$ ./iAmNotFrontendDev.sh clean uploadArchives -x test
```

Build all the java projects included in the Sailfish
Execute command from the repository root folder
```
$ ./gradlew clean build -x test
```

Publish a plugins example into the Sailfish workspace layer (shared_workspace folder in the repository root).
Execute command from the repository root folder
```
$ ./gradlew cleanSharedWorkspace publishPlugin
```

Run Sailfish by cargo plugin.
Execute command from the repository root folder
```
$ ./gradlew cargoRunLocal -PquickStart
```
Note: The 'quickStart' property enables the ability to run Sailfish without connecting to a DBMS.