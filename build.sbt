org.scalastyle.sbt.ScalastylePlugin.Settings

name := "trabot"

version := "0.1"

organization := "com.openquant"

scalaVersion := "2.11.5"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

// stackoverflow.com/questions/24310889/how-to-redirect-aws-sdk-logging-output
resolvers += "version99 Empty loggers" at "http://version99.qos.ch"

//resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

libraryDependencies ++= {
  Seq(
    "org.slf4j" % "jcl-over-slf4j" % "1.7.7",
    "org.specs2" %% "specs2" % "2.3.12" % "test",
    "commons-logging" % "commons-logging" % "99-empty",
    "com.github.scopt" %% "scopt" % "3.2.0",
    "com.github.seratch" %% "awscala" % "0.2.+" excludeAll(ExclusionRule(organization= "org.apache.commons.logging")),
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "joda-time" % "joda-time" % "2.9",
    "org.scalikejdbc" %% "scalikejdbc" % "2.2.+",
    "org.xerial" % "sqlite-jdbc" % "3.8.7",
    "com.github.tototoshi" %% "scala-csv" % "1.1.2",
    "com.typesafe" % "config" % "1.2.1",
    "net.ceedubs" %% "ficus" % "1.1.2",
    "com.yahoofinance-api" % "YahooFinanceAPI" % "2.0.0",
    "org.slf4j" % "jul-to-slf4j" % "1.7.12",
    "org.slf4j" % "log4j-over-slf4j" % "1.7.12"
  )
}

resolvers += Resolver.sonatypeRepo("public")

testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "console", "junitxml")


parallelExecution in Test := false

assemblyMergeStrategy in assembly := {
  case "application.conf" => MergeStrategy.concat
  case "logback.xml" => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

