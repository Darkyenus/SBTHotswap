# SBTHotswap

## How to use
1. Make sure that you have [sbt](http://www.scala-sbt.org) installed. This is a sbt plugin.
2. Clone/download this repository
3. In this directory, run `sbt +publish-local`, that will publish this plugin into your local ivy repo
4. To `project/plugins.sbt` file in your project add `addSbtPlugin("darkyenus" %% "sbthotswap" % "0.0-SNAPSHOT")`
5. Run your project in hotswap mode with `sbt hotswap`
6. Do changes to your code, save and changes will be automatically reloaded at runtime

## Limitations
- This plugin uses standard `java.lang.instrument.Instrumentation` to redefine classes. This means:
  - Only some changes can be hotswapped, what exactly is dependent on your JVM, but changing code in method body should always work
  - This may not be supported on all JVMs (major ones should be fine)

## Advanced
- `hotswap` task is actually an alias for `~hotswap_i`, so call `hotswap_i` repeatedly for finer control over when the hotswaps will happen
- Plugin adds a java agent when creating a new process and communicates with that agent using a socket. Port of that communication can be changed, for example `javaAgentPort := 7777`. Default is 5011.
- Only changing files is supported, removing and adding is not (adding may theoretically work however).

## License
Under MIT license, see LICENSE file.
Note that parts of this plugin (package gnu) are from [Kawa](https://www.gnu.org/software/kawa/) and they are under separate license (see COPYING, in src/main/java/gnu).
Package gnu.bytecode (with dependencies in gnu.utils) is used for class name detection of .class files.
