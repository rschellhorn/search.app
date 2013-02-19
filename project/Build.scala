import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName = "searchapp"
    val appVersion = "1.0-SNAPSHOT"

    val appDependencies = Seq(
        "org.webjars" % "webjars-play" % "2.1.0-1",
        "org.webjars" % "angular-strap" % "0.7.2-1",
        "org.webjars" % "highcharts" % "3.0.1"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings()
}