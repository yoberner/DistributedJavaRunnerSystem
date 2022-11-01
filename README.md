# DistributedJavaRunnerSystem

## Distributed, Fault-Tolerant, Serverless Computing Java Program

### Fall 2021 Distributed Systems Semester Project

> Overview of the Project:

> Serverless Computing is a model of cloud computing in which the user of the service does not manage the computer / virtual
> machine on which their code runs. Instead, the user simply uploads his code to the cloud service, which takes care of all the
> setup and management necessary to run the code. Commercial examples of serverless include AWS Lambda, Microsoft Azure
> Functions, Google Cloud Functions and App Engine, and Cloudflare Workers.

> In this project you will build, mostly from scratch, a service which provides serverless execution of Java code. You will start out
> with building the simplest form of distributed system – a single client talking to a single server – and work all the way up to a
> scalable fault-tolerant cluster. It will involve both applying concepts we learn in class as well as independent learning (including
> trial and error) regarding how to do specific things in Java.

---

* Fault tolerant distributed cluster which accepts Java source code via REST API from client and compiles and runs it.
* Implemented Zookeeper Leader Election and Heartbeat Protocol to recover from node failure and ensure fault tolerance.
* Server to server communication utilizes UDP and TCP protocols.
* Client to server communication utilizes HTTP protocol.
* Allows for redistribution of work among the cluster if a server node goes down.
