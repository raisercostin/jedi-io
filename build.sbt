organization := "org.raisercostin"
name := "jedi-io"
version := "0.10"
description := "Scala fluent file utility library"
homepage := Some(url(s"https://github.com/raisercostin/"+name.value))

scalaVersion := "2.10.5"
//crossScalaVersions := Seq(scalaVersion.value, "2.11.4")
scalacOptions ++= Seq(Opts.compile.deprecation, "-feature")

libraryDependencies ++= Seq(
	"net.sf.jopt-simple" % "jopt-simple" % "2.4.1" intransitive() //exclude("org.apache.ant" % "ant")
	,"dom4j" % "dom4j" % "1.6.1"
	,"jaxen" % "jaxen" % "1.1.6"
	,"org.scalatest" %% "scalatest" % "2.2.4" //% "test"
	,"junit" % "junit" % "4.10" //% "test"
	,"org.slf4j" % "slf4j-api" % "1.7.5"
	,"org.slf4j" % "slf4j-simple" % "1.7.5"
	,"commons-io" % "commons-io" % "2.4"
)

// This is an example.  bintray-sbt requires licenses to be specified 
// (using a canonical name).
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
resolvers += "raisercostin" at "http://dl.bintray.com/raisercostin/maven"
pomExtra := (
  <scm>
    <url>git@github.com:raisercostin/{name.value}.git</url>
    <connection>scm:git:git@github.com:raisercostin/{name.value}.git</connection>
  </scm>
  <developers>
    <developer>
      <id>raisercostin</id>
      <name>raisercostin</name>
      <url>https://github.com/raisercostin</url>
    </developer>
  </developers>
)

EclipseKeys.eclipseOutput := Some("target2/eclipse")

//bintray
publishMavenStyle := true
bintrayPackageLabels := Seq("scala", "io", "nio", "file", "path", "stream", "writer")
