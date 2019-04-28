package com.ramijemli.vision

import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3

class TranslatableNode : Node() {

  fun addOffset(x: Float = 0F, y: Float = 0F, z: Float = 0F) {
    val posX = localPosition.x + x
    val posY = localPosition.y + y
    val posZ = localPosition.z + z

    localPosition = Vector3(posX, posY, posZ)
  }
}
