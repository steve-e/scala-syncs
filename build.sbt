ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.17"

lazy val artifactoryUrl              = "artifactory.mpi-internal.com"
lazy val artifactoryResolver         = "Artifactory Realm Libs" at s"https://$artifactoryUrl/artifactory/libs-release/"
lazy val artifactorySnapshotResolver = "Artifactory Snapshot Realm Libs" at s"https://$artifactoryUrl/artifactory/libs-snapshot/"
lazy val artifactoryCredentials =
  Credentials(
    "Artifactory Realm",
    artifactoryUrl,
    System.getenv("ARTIFACTORY_USER"),
    System.getenv("ARTIFACTORY_PWD")
  )

lazy val artifactorySettings = List(
  resolvers ++= List(artifactoryResolver, artifactorySnapshotResolver, Resolver.jcenterRepo, Resolver.mavenLocal),
  credentials += artifactoryCredentials
)


addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

lazy val root = (project in file("."))
  .settings(
    name := "scala-syncs",
    scalacOptions ++= Seq("-Ypartial-unification", "-Yrangepos"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.1.1",
      "org.typelevel" %% "cats-effect" % "2.1.2",
      "org.typelevel" %% "simulacrum" % "1.0.1",
      "org.scalatest" %% "scalatest" % "3.2.9",
      "org.scalameta" %% "munit" % "0.7.29",
      "org.typelevel" %% "munit-cats-effect-2" % "1.0.7"
    ),
   artifactorySettings
  )

lazy val docs = project
  .in(file("mdocs-source")) // important: it must not be docs/
  .settings(
    scalacOptions += "-Ypartial-unification",
    mdocOut := file("docs"),
    mdocIn := file("mdocs-source"),
    mdocExtraArguments := List("--no-link-hygiene")
  )
  .dependsOn(root)
  .enablePlugins(MdocPlugin)

