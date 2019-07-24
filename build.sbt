name := "cats-effect-tutorial"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= {
  val catsVersion = "1.3.1"

  Seq(
    "org.typelevel" %% "cats-effect" % catsVersion withSources() withJavadoc(),
    "dev.zio" %% "zio" % "1.0.0-RC10-1" withSources() withJavadoc(),
  )
}

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds",
  "-Ypartial-unification",
)
