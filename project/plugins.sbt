logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

//addSbtPlugin("com.jamesward" % "play-auto-refresh" % "0.0.15")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.7")
