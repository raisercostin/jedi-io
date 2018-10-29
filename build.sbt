organization := "org.raisercostin"
name := "jedi-io"
description := "Scala fluent file utility library"
homepage := Some(url(s"https://github.com/raisercostin/"+name.value))

//scalaVersion := "2.10.6"
scalaVersion := "2.11.11"
crossScalaVersions := Set(scalaVersion.value, "2.10.6","2.11.11","2.12.3"
    /*,"2.13.0-M2"
    //rxscala, scalaj_http, scalatest are not yet compiled for 2.13
    */
    ).toSeq
scalacOptions ++= Seq(Opts.compile.deprecation, "-feature")

libraryDependencies ++= Seq(
	"ch.qos.logback" % "logback-classic" % "1.2.3"
	,"commons-io" % "commons-io" % "2.5"
	,"org.slf4j" % "slf4j-api" % "1.7.5"
	,"org.scalatest" %% "scalatest" % "3.0.1" % Test
	,"junit" % "junit" % "4.12" % Test
	//,"org.slf4j" % "slf4j-simple" % "1.7.5" % Test
	,"org.apache.commons" % "commons-vfs2" % "2.1" % "optional"
  	,"commons-httpclient" % "commons-httpclient" % "3.1" % "optional"
  	,"commons-codec" % "commons-codec" % "1.10" % "optional"
  	//,"org.apache.httpcomponents" % "httpclient" % "4.0" % "optional" //next versions of httpclient but not used by commons-vfs2
	//,"org.apache.jackrabbit" % "jackrabbit-webdav" % "1.5.2"
	,"org.kohsuke" % "file-leak-detector" % "1.8" % "optional"
	,"io.reactivex" %% "rxscala" % "0.26.5"
	,"io.reactivex.rxjava2" % "rxjava" % "2.2.3"
	,"org.scalaj" %% "scalaj-http" % "2.3.0"
	,"org.apache.commons" % "commons-email" % "1.4" excludeAll (
	         ExclusionRule(organization="com.sun.mail", name= "javax.mail")
	         ,ExclusionRule(organization="javax.activation", name="activation")
	        )
	, "com.sun.mail" % "javax.mail" % "1.5.5"
	,"com.google.guava" % "guava"%"26.0-jre"
  ,"org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0"
)

/*
libraryDependencies +=
  "log4jdbc" % "log4jdbc" % "1.1" % "compile,optional" from
    "http://log4jdbc.googlecode.com/files/log4jdbc4-1.1.jar"
 <!-- 3rd party dependencies -->
      <dependency>
        <groupId>commons-logging</groupId>
        <artifactId>commons-logging</artifactId>
        <version>1.1.1</version>
      </dependency>
      <dependency>
        <groupId>commons-net</groupId>
        <artifactId>commons-net</artifactId>
        <version>2.2</version>
      </dependency>
      <dependency>
        <groupId>commons-collections</groupId>
        <artifactId>commons-collections</artifactId>
        <version>3.1</version>
      </dependency>
      <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-compress</artifactId>
        <version>1.1</version>
      </dependency>
      <dependency>
        <groupId>org.apache.jackrabbit</groupId>
        <artifactId>jackrabbit-webdav</artifactId>
        <version>1.5.2</version>
      </dependency>
      <dependency>
        <groupId>ant</groupId>
        <artifactId>ant</artifactId>
        <version>1.6.2</version>
      </dependency>
      <dependency>
        <groupId>org.jdom</groupId>
        <artifactId>jdom</artifactId>
        <version>1.1</version>
      </dependency>
      <dependency>
        <groupId>com.jcraft</groupId>
        <artifactId>jsch</artifactId>
        <version>0.1.42</version>
      </dependency>
      <dependency>
        <groupId>jcifs</groupId>
        <artifactId>jcifs</artifactId>
        <version>0.8.3</version>
      </dependency>
      <dependency>
        <groupId>javax.mail</groupId>
        <artifactId>mail</artifactId>
        <version>1.4</version>
      </dependency>
      <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.8.2</version>
        <scope>test</scope>
      </dependency>
*/
	  
// This is an example.  bintray-sbt requires licenses to be specified
// (using a canonical name).
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
resolvers += "raisercostin resolver" at "http://dl.bintray.com/raisercostin/maven"
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

//eclipse
EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
EclipseKeys.withSource := true
EclipseKeys.eclipseOutput := Some("target2/eclipse")
unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil
unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil
//to enable full stacktraces for ScalaTest
testOptions in Test += Tests.Argument("-oF")

//style
//scalariformSettings
scalastyleConfig := baseDirectory.value / "project" / "scalastyle_config.xml"

//bintray
publishMavenStyle := true
bintrayPackageLabels := Seq("scala", "io", "nio", "file", "path", "stream", "writer")

//release plugin
//version is commented since the version is in version.sbt
releaseCrossBuild := true

//bintray&release
//bintray doesn't like snapshot versions - https://github.com/softprops/bintray-sbt/issues/12
releaseNextVersion := { ver => sbtrelease.Version(ver).map(_.bumpMinor.string).getOrElse(sbtrelease.versionFormatError) }

//coverage: https://github.com/scoverage/sbt-scoverage and https://github.com/non/spire/blob/master/.travis.yml
//instrumentSettings
//ScoverageKeys.minimumCoverage := 60
//ScoverageKeys.failOnMinimumCoverage := false
//ScoverageKeys.highlighting := {
//  if (scalaBinaryVersion.value == "2.10") false
//  else false
//}
//scapegoatVersion := "1.1.0"


ensimeIgnoreScalaMismatch in ThisBuild := true