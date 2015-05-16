sbtPlugin := true

name := "SBTHotswap"

organization := "darkyenus"

version := "1.0"

scalaVersion := "2.11.5"

startYear := Some(2015)

crossScalaVersions in Compile := Seq(scalaVersion.value, "2.10.4")

packageOptions in Compile += Package.ManifestAttributes(
  "Agent-Class" -> "darkyenus.sbthotswap.agent.AgentMain",
  "Premain-Class" -> "darkyenus.sbthotswap.agent.AgentMain",
  "Can-Redefine-Classes" -> "true")