import bintray.Keys._

organization := "org.raisercostin"

name := "raisercostin-utils"

version := "0.1"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
	"net.sf.jopt-simple" % "jopt-simple" % "2.4.1" intransitive() //exclude("org.apache.ant" % "ant")
	,"dom4j" % "dom4j" % "1.6.1"
	,"jaxen" % "jaxen" % "1.1.6"
	,"org.scalatest" %% "scalatest" % "2.0" //% "test"
	,"junit" % "junit" % "4.10" //% "test"
	,"org.slf4j" % "slf4j-api" % "1.7.5"
	,"org.slf4j" % "slf4j-simple" % "1.7.5"
	,"commons-io" % "commons-io" % "2.4"
)

sbtPlugin := true

publishMavenStyle := true

bintrayPublishSettings

repository in bintray := "generic"

// This is an example.  bintray-sbt requires licenses to be specified 
// (using a canonical name).
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization in bintray := None

resolvers += "raisercostin" at "https://raisercostin.googlecode.com/svn/maven2"