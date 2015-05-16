package darkyenus.sbthotswap

import java.io.{DataOutputStream, File}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 *
 * @author Darkyen
 */
final class CPTracker() {

  private val files = new mutable.HashMap[String, Long]()

  private def canBeInCP(file:File): Boolean ={
    val name = file.getName.toLowerCase
    if(name.startsWith(".") || name.endsWith(".jar")){
      //Ignore hidden files and jars
      false
    }else{
      true
    }
  }

  private def addToCP(cp:ArrayBuffer[File], directory:File): Unit ={
    for(f <- directory.listFiles() if canBeInCP(f)){
      if(f.isFile){
        cp += f
      }else if(f.isDirectory){
        addToCP(cp, f)
      }
    }
  }

  private def toCP(cp:Seq[File]):Seq[File] = {
    val result = new ArrayBuffer[File]()

    for(f <- cp if canBeInCP(f)){
      if(f.isFile){
        result += f
      }else if(f.isDirectory){
        addToCP(result, f)
      }
    }

    result
  }

  def this(cp:Seq[File]){
    this()

    for(file <- toCP(cp)){
      files(file.getCanonicalPath) = file.lastModified()
    }
  }

  class UpdateResult(val changed:Seq[File], val added:Seq[File]){

    override def toString = {
      val b = new StringBuilder("UpdateResult\n")
      if(changed.isEmpty){
        b.append("\tNo files changed\n")
      }else{
        b.append('\t').append(changed.length).append(" file(s) changed\n")
        for(c <- changed){
          b.append("\t\t").append(c.getName).append('\n')
        }
      }

      if(added.isEmpty){
        b.append("\tNo files added\n")
      }else{
        b.append('\t').append(added.length).append(" file(s) added\n")
        for(c <- added){
          b.append("\t\t").append(c.getName).append('\n')
        }
      }

      b.toString()
    }

    def sendToAgent(out:DataOutputStream): Unit ={
      if(changed.nonEmpty){
        for(c <- changed){
          out.writeByte(ProtocolActions.FILE_CHANGED)
          out.writeUTF(c.getCanonicalPath)
        }
        out.writeByte(ProtocolActions.FLUSH)
        out.flush()
      }
    }
  }

  def update(newCP:Seq[File]):UpdateResult = {
    val changed = new ArrayBuffer[File]()
    val added = new ArrayBuffer[File]()

    //(Ignoring removed files for now)
    for(file <- toCP(newCP)){
      val key = file.getCanonicalPath
      val modified = file.lastModified()

      files.get(key) match {
        case Some(`modified`) => //Still the same
        case Some(differentModified) =>
          //File changed
          files(key) = modified
          changed += file
        case None =>
          //File added
          files(key) = modified
          added += file
      }
    }

    new UpdateResult(changed,added)
  }

}
