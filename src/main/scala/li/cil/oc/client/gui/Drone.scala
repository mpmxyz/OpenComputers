package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.font.TextBufferRenderData
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.entity
import li.cil.oc.util.PackedColor
import li.cil.oc.util.RenderState
import li.cil.oc.util.TextBuffer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.InventoryPlayer
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsJava._

class Drone(playerInventory: InventoryPlayer, val drone: entity.Drone) extends DynamicGuiContainer(new container.Drone(playerInventory, drone)) with traits.DisplayBuffer {
  xSize = 176
  ySize = 148

  protected var powerButton: ImageButton = _

  private val buffer = new TextBuffer(20, 2, new PackedColor.SingleBitFormat(0x33FF33))
  private val bufferRenderer = new TextBufferRenderData {
    private var _dirty = true

    override def dirty = _dirty

    override def dirty_=(value: Boolean) = _dirty = value

    override def data = buffer

    override def viewport: (Int, Int) = buffer.size
  }

  override protected val bufferX = 9
  override protected val bufferY = 9
  override protected val bufferColumns = 80
  override protected val bufferRows = 16

  private val inventoryX = 97
  private val inventoryY = 7

  private val power = addWidget(new ProgressBar(28, 48))

  private val selectionSize = 20
  private val selectionsStates = 17
  private val selectionStepV = 1 / selectionsStates.toDouble

  protected override def actionPerformed(button: GuiButton) {
    if (button.id == 0) {
      ClientPacketSender.sendDronePower(drone, !drone.isRunning)
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    powerButton.toggled = drone.isRunning
    bufferRenderer.dirty = drone.statusText.lines.zipWithIndex.exists {
      case (line, i) => buffer.set(0, i, line, vertical = false)
    }
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    powerButton = new ImageButton(0, guiLeft + 7, guiTop + 45, 18, 18, Textures.guiButtonPower, canToggle = true)
    add(buttonList, powerButton)
  }

  override protected def drawBuffer() {
    GL11.glTranslatef(bufferX, bufferY, 0)
    RenderState.disableLighting()
    RenderState.makeItBlend()
    GL11.glScaled(scale, scale, 1)
    GL11.glPushAttrib(GL11.GL_DEPTH_BUFFER_BIT)
    GL11.glDepthMask(false)
    GL11.glColor3f(0.5f, 0.5f, 1f)
    TextBufferRenderCache.render(bufferRenderer)
    GL11.glPopAttrib()
  }

  override protected def changeSize(w: Double, h: Double, recompile: Boolean) = 2.0

  override protected def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) {
    drawBufferLayer()
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS) // Me lazy... prevents NEI render glitch.
    if (func_146978_c(power.x, power.y, power.width, power.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      val format = Localization.Computer.Power + ": %d%% (%d/%d)"
      tooltip.add(format.format(
        drone.globalBuffer * 100 / math.max(drone.globalBufferSize, 1),
        drone.globalBuffer,
        drone.globalBufferSize))
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }
    if (powerButton.func_146115_a) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll(asJavaCollection(if (drone.isRunning) Localization.Computer.TurnOff.lines.toIterable else Localization.Computer.TurnOn.lines.toIterable))
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRendererObj)
    }
    GL11.glPopAttrib()
  }

  override protected def drawGuiContainerBackgroundLayer(dt: Float, mouseX: Int, mouseY: Int) {
    GL11.glColor3f(1, 1, 1) // Required under Linux.
    mc.renderEngine.bindTexture(Textures.guiDrone)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    power.level = drone.globalBuffer.toDouble / math.max(drone.globalBufferSize.toDouble, 1.0)
    drawWidgets()
    if (drone.mainInventory.getSizeInventory > 0) {
      drawSelection()
    }

    drawInventorySlots()
  }

  protected override def drawGradientRect(par1: Int, par2: Int, par3: Int, par4: Int, par5: Int, par6: Int) {
    super.drawGradientRect(par1, par2, par3, par4, par5, par6)
    RenderState.makeItBlend()
  }

  // No custom slots, we just extend DynamicGuiContainer for the highlighting.
  override protected def drawSlotBackground(x: Int, y: Int) {}

  private def drawSelection() {
    val slot = drone.selectedSlot
    if (slot >= 0 && slot < 16) {
      RenderState.makeItBlend()
      Minecraft.getMinecraft.renderEngine.bindTexture(Textures.guiRobotSelection)
      val now = System.currentTimeMillis() / 1000.0
      val offsetV = ((now - now.toInt) * selectionsStates).toInt * selectionStepV
      val x = guiLeft + inventoryX - 1 + (slot % 4) * (selectionSize - 2)
      val y = guiTop + inventoryY - 1 + (slot / 4) * (selectionSize - 2)

      val t = Tessellator.instance
      t.startDrawingQuads()
      t.addVertexWithUV(x, y, zLevel, 0, offsetV)
      t.addVertexWithUV(x, y + selectionSize, zLevel, 0, offsetV + selectionStepV)
      t.addVertexWithUV(x + selectionSize, y + selectionSize, zLevel, 1, offsetV + selectionStepV)
      t.addVertexWithUV(x + selectionSize, y, zLevel, 1, offsetV)
      t.draw()
    }
  }
}