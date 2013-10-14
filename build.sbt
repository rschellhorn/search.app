name := "searchapp"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
    "com.googlecode.ez-vcard" % "ez-vcard" % "0.8.5",
    "org.webjars" % "angular-strap" % "0.7.4",
    "org.webjars" % "highcharts" % "3.0.1",
    "org.webjars" %% "webjars-play" % "2.2.0"
)

play.Project.playScalaSettings
