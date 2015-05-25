package darkyenus.sbthotswap

import java.io.DataOutputStream
import java.net.{ConnectException, Socket}

import sbt.Keys._
import sbt._

import scala.util.Try

/**
 * Adds task for running with this plugin as agent, which will attempt to hotswap on calling "hotswap".
 */
object SbtHotswap extends AutoPlugin {
  /*
  -javaagent:jarpath[=options]
   */

  private var processRunning = false
  @volatile
  private var processFinished = false
  @volatile
  private var processExitStatus = -1

  private var cpTracker:CPTracker = null

  private var hotswapSocket:Socket = null
  private var hotswapSocketOut:DataOutputStream = null

  // (special name)
  object autoImport {
    val hotswap_i = taskKey[Unit]("Recompiles and hotswaps the code into the running application.")
    val javaAgentOption = settingKey[Seq[String]]("-javaagent: type string that goes into the hotswap java options")
    val javaAgentPort = settingKey[Int]("Port on which hotswap communication will run")

    lazy val baseHotswapSettings = Seq(
      javaAgentPort := 5011,
      javaAgentOption := {
        Seq(s"-javaagent:${file(getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath).getCanonicalPath}=${javaAgentPort.value}")
      },
      hotswap_i := {
        val log = streams.value.log
        val fullCP = (fullClasspath in (Compile, hotswap_i)).value

        if(processRunning && processFinished){
          log.info("Hotswap run finished with exit code "+processExitStatus)
          processRunning = false
          processFinished = false
          processExitStatus = -1
          Try(hotswapSocket.close())
          hotswapSocket = null
          hotswapSocketOut = null
        }
        
        if(!processRunning){
          val cp = fullCP.map(_.data).mkString(":")
          val fo = new ForkOptions(
            workingDirectory = Some((baseDirectory in (Compile, hotswap_i )).value),
            runJVMOptions = javaAgentOption.value ++ Seq("-cp", cp, (mainClass in (Compile, hotswap_i)).value.get))
          val javaProcess = Fork.java.fork(fo,Nil)

          new Thread("SBT Hotswap - Waiting For Process"){
            override def run(): Unit = {
              processExitStatus = javaProcess.exitValue()
              processFinished = true
            }
          }.start()
          processRunning = true
          cpTracker = new CPTracker(fullCP.map(_.data))

          hotswapSocket = null
          while(hotswapSocket == null || !hotswapSocket.isBound || !hotswapSocket.isConnected){
            if(hotswapSocket != null){
              Try(hotswapSocket.close())
            }
            try{
              hotswapSocket = new Socket("127.0.0.1",javaAgentPort.value)
            }catch {
              case e:ConnectException =>
                log.debug("Failed to connect: "+e)
                Thread.sleep(1000)
            }
          }

          hotswapSocketOut = new DataOutputStream(hotswapSocket.getOutputStream)

          log.info("Hotswap run started")
        }else{
          //Do a hotswap

          log.info("Hotswapping")
          val updateResult = cpTracker.update(fullCP.map(_.data))
          log.debug(updateResult.toString)

          updateResult.sendToAgent(hotswapSocketOut)
        }
      }
    )
  }

  import autoImport._

  override def requires: Plugins = sbt.plugins.JvmPlugin

  override def trigger: PluginTrigger = allRequirements

  override lazy val projectSettings = inConfig(Compile)(baseHotswapSettings ++ addCommandAlias("hotswap","~hotswap_i"))
}

