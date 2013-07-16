import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName = "searchapp"
    val appVersion = "1.0-SNAPSHOT"

    val appDependencies = Seq(
        "com.googlecode.ez-vcard" % "ez-vcard" % "0.8.2",
        "org.webjars" % "angular-strap" % "0.7.4",
        "org.webjars" % "highcharts" % "3.0.1",
        "org.webjars" %% "webjars-play" % "2.1.0-2"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings()
}