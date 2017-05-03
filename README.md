# Jedi IO

## Status
[![Download](https://api.bintray.com/packages/raisercostin/maven/jedi-io/images/download.svg)](https://bintray.com/raisercostin/maven/jedi-io/_latestVersion)
[![Build Status](https://travis-ci.org/raisercostin/jedi-io.svg?branch=master)](https://travis-ci.org/raisercostin/jedi-io)
[![Codacy Badge](https://www.codacy.com/project/badge/5cc4b6b21f694317ab8beec05342c7b5)](https://www.codacy.com/app/raisercostin/jedi-io)
[![codecov](https://codecov.io/gh/raisercostin/jedi-io/branch/master/graph/badge.svg)](https://codecov.io/gh/raisercostin/jedi-io)
<!--[![codecov.io](http://codecov.io/github/raisercostin/jedi-io/coverage.svg?branch=master)](http://codecov.io/github/raisercostin/jedi-io?branch=master)-->

## Description
Uniform, fluent access to files, urls and other resources API from java and scala.
The main purpose of this project is to provide uniform, fluent access to various input and output data locations.

## Features
- integrate apache commons vfs via Locations.vfs("...")
- UrlLocation transparently manages
  - handle redirects
  - good RequestHeader defaults
  - reuse already opened HttpConnections
  - use proxy if using scalaj/http library
  - manage backpresure from server
    - [TODO] connection timeout temporarly
    - [TODO] maximum connections/ip-hostname? for a specified timeframe?
    - [TODO] delay between requests to same server
  - [TODO] non-blocking IO returning Future/Observable ?

# Usage
## Samples
 ```
	//Reading from a file:
	Locations.file("/home/costin/myfile.txt").readContent
 
	//Copying a file to a new folder (and create parent folder if needed)
	Locations.file("/home/costin/myfile.txt").
		copyTo(Locations.file("/home/costin/folder2/myfile2.txt").mkdirOnParentIfNecessary))

 	//Copying a file to a new relative folder (and create parent folder if needed)
	Locations.file("/home/costin/myfile.txt").
		copyTo(Locations.relative("folder3/myfile2.txt")))

	//read content from classpath
    val text = Locations.classpath("META-INF/maven/org.slf4j/slf4j-api/pom.properties").
		readContentAsText.get

	//get a stream  from classpath
    val text = Locations.classpath("META-INF/maven/org.slf4j/slf4j-api/pom.properties").toInputStream
 ```

For more samples see [LocationsTest.scala](src/test/scala/org/raisercostin/util/io/LocationsTest.scala)

## Library
 - from sbt

 ```
 libraryDependencies += "org.raisercostin" %% "jedi-io" % "0.18"
 ```
 - maven resolver at bintray - http://dl.bintray.com/raisercostin/maven

 ```
 resolvers += "raisercostin repository" at "http://dl.bintray.com/raisercostin/maven"
 ```

# Development

Projects that are using jedi-io:
 - https://github.com/raisercostin/ownit
 - https://github.com/raisercostin/my-scala-scripts (see here a script for bulk uploading/importing maven artefacts from a repository/svn/folder to bintray)
 - to configure release
     ```bintrayChangeCredentials```
 - to release

 ```
 sbt> release skip-tests
 ```

## Roadmap
 - investigate
   - scala arm - http://jsuereth.com/scala-arm/continuations.html
   - scala io - https://github.com/scala-incubator/scala-io
   - spray - 
   - akka streams -
 - make it async

## Backlog
 - Locations.url("file://...") should create a FileLocation?
 - add FileStore

## Resources
 - http://javapapers.com/java/file-attributes-using-java-nio/
 - https://docs.oracle.com/javase/tutorial/essential/io/fileAttr.html
 - http://www.javacodex.com/More-Examples/1/8
