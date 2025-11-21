# gridcapa-core-valid-day-ahead-conservative-post-processor
It provides a suite to perform aggregate calculations on CORE zone, previously done by RAMCEP2

## Functional overview
The Clean Energy Package CEP 70% project provides for increases in exchange capacity at interconnections in the CORE region.
The aim of this application is to validate these capacity levels by studying the effect of the increasing capacity on certain study-points of interest of the flow-based domain.

## Developer documentation

## Build application

Application is using Maven as base build framework. Application is simply built with following command.

```bash
mvn install
```

## Build docker image

For building Docker image of the application, start by building application.

```bash
mvn install
```

Then build docker image

```bash
docker build -t farao/gridcapa-core-valid-day-ahead-conservative-post-processing .
```
