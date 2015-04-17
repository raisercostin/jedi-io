# input/output locations #

The main purpose of this project is to provide uniform, fluent access to various input and output data locations.

## Samples ##
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

## Releasing ##
 ```
 mvn release:prepare release:perform -Prelease -DskipTests -Darguments="-DskipTests -Prelease"
 ```