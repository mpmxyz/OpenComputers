package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Settings
import li.cil.oc.api.event.FileSystemAccessEvent
import li.cil.oc.api.internal.Rack
import li.cil.oc.common.tileentity.Case
import li.cil.oc.common.tileentity.DiskDrive
import li.cil.oc.common.tileentity.Raid
import li.cil.oc.server.component.DiskDriveMountable
import li.cil.oc.server.component.Server

object FileSystemAccessHandler {
  @SubscribeEvent
  def onFileSystemAccess(e: FileSystemAccessEvent.Server) {
    e.getTileEntity match {
      case t: Rack =>
        for (slot <- 0 until t.getSizeInventory) {
          t.getMountable(slot) match {
            case server: Server =>
              val containsNode = server.componentSlot(e.getNode.address) >= 0
              if (containsNode) {
                server.lastFileSystemAccess = System.currentTimeMillis()
                t.markChanged(slot)
              }
            case diskDrive: DiskDriveMountable =>
              val containsNode = diskDrive.filesystemNode.contains(e.getNode)
              if (containsNode) {
                diskDrive.lastAccess = System.currentTimeMillis()
                t.markChanged(slot)
              }
            case _ =>
          }
        }
      case _ =>
    }
  }

  @SubscribeEvent
  def onFileSystemAccess(e: FileSystemAccessEvent.Client) {
    val volume = Settings.get.soundVolume
    e.getWorld.playSound(e.getX, e.getY, e.getZ, e.getSound, volume, 1, false)
    e.getTileEntity match {
      case t: DiskDrive => t.lastAccess = System.currentTimeMillis()
      case t: Case => t.lastFileSystemAccess = System.currentTimeMillis()
      case t: Raid => t.lastAccess = System.currentTimeMillis()
      case _ =>
    }
  }
}
