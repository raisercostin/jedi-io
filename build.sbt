organization := "org.raisercostin"
name := "jedi-io"
description := "Scala fluent file utility library"
homepage := Some(url(s"https://github.com/raisercostin/"+name.value))

//scalaVersion := "2.10.5"
scalaVersion := "2.11.2"
//crossScalaVersions := Seq(scalaVersion.value, "2.11.4")
scalacOptions ++= Seq(Opts.compile.deprecation, "-feature")

libraryDependencies ++= Seq(
	"commons-io" % "commons-io" % "2.4"
	,"org.slf4j" % "slf4j-api" % "1.7.5"
	,"org.scalatest" %% "scalatest" % "2.2.4" % "test"
	,"junit" % "junit" % "4.10" % "test"
	,"org.slf4j" % "slf4j-simple" % "1.7.5" % "test"
)

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

//style
//scalariformSettings
scalastyleConfig := baseDirectory.value / "project" / "scalastyle_config.xml"

//bintray
publishMavenStyle := true
bintrayPackageLabels := Seq("scala", "io", "nio", "file", "path", "stream", "writer")

//release plugin
//version is commented since the version is in version.sbt
releaseCrossBuild := false

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