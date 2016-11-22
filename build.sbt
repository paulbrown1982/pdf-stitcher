name := "pdf-stitcher"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test",
  "org.apache.pdfbox" % "pdfbox" % "2.0.3"
)
