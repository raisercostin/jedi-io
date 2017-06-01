# Jedi IO

## Status
[![Download](https://api.bintray.com/packages/raisercostin/maven/jedi-io/images/download.svg)](https://bintray.com/raisercostin/maven/jedi-io/_latestVersion)
[![Build Status](https://travis-ci.org/raisercostin/jedi-io.svg?branch=master)](https://travis-ci.org/raisercostin/jedi-io)
[![Codacy Badge](https://www.codacy.com/project/badge/5cc4b6b21f694317ab8beec05342c7b5)](https://www.codacy.com/app/raisercostin/jedi-io)
[![codecov](https://codecov.io/gh/raisercostin/jedi-io/branch/master/graph/badge.svg)](https://codecov.io/gh/raisercostin/jedi-io)
<!--[![codecov.io](http://codecov.io/github/raisercostin/jedi-io/coverage.svg?branch=master)](http://codecov.io/github/raisercostin/jedi-io?branch=master)-->

## Description
Scala uniform, fluent access to files, urls and other resources API. Fluent for java too.

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
- Version and Etag that is changed if file is changed. A change version/tag doesn't warranty a change in file content.
- Types of Locations
  - Abstract Locations
    - InputLocation - locations that can be read
    - OutputLocation - locations that can be written
    - InOutLocation - location that can be read/written 
    - RelativeLocation - part of a location. Cannot be resolved to some content.
    - NavigableLocation - location for which you can find parent/childrens/descendants.
    - VersionedLocation - location trait with Version/Etag/UniqueId 
  - Physical Locations
    - FileLocation - HasContent, NoChildren
    - FolderLocation - NoContent, HasChildren
    - MemoryLocation - read/write in memory content - useful for tests.
    - ClasspathLocation - InputLocation from classpath.
    - Stream Location - location from a InputStream. Might not be reopened.
    - TempLocation - location in the temporary file system.
    - UrlLocation - location from a url. Follows redirects if needed
    - VfsLocation - location based on [Apache-Vfs library](https://commons.apache.org/proper/commons-vfs/filesystems.html)
    - ZipInputLocation - location around zip files
- natural sorting for listing files

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

## Backlog
 - explain concepts
 - add File/Folder concepts
 - operations on files/folders
 - operations on lists of files/folders (like manual selections or filters in OFM)
 - make it async
 - make a small 2panel file manager - see trolCommander - [other file managers](https://en.wikipedia.org/wiki/Comparison_of_file_managers)
 - Locations.url("file://...") should create a FileLocation?
 - add FileStore
 - AddHttpsWritable via vfs - http://detailfocused.blogspot.ro/2009/06/add-plugin-for-apache-vfs.html
 - add Locations:
   - StreamProviderLocation - location that knows how to open a stream
   - CachedLocation - location that reads the original location only if the cache expired. Useful around UrlLocation, ZipLocation, etc. Check version if changed read origin. 
   - MetadataLocation - location that saves metadata associated with the file. See osx files, ds-
 - investigate
   - scala arm - http://jsuereth.com/scala-arm/continuations.html
   - scala io - https://github.com/scala-incubator/scala-io
   - spray - 
   - akka streams -
 - UrlLocation use RequestHeader for mime type and reader encoding. Maybe we need MetadataLocation 
 - clarify operations
   - do actions (need to resolve to the filesystems)
   - just change in memory representation
 - both Resolved/Absolute and Relative could act as destination if they are resolved with src. `absolute.asDestination(src)=>absolute2` and `relative.asDestination(src)=>absolute3`

## Resources
 - http://javapapers.com/java/file-attributes-using-java-nio/
 - https://docs.oracle.com/javase/tutorial/essential/io/fileAttr.html
 - http://www.javacodex.com/More-Examples/1/8
