package net.ccbluex.liquidbounce.features.module.modules.render

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.liquidbounce.utils.render.readNativeImage
import net.ccbluex.liquidbounce.utils.render.registerTexture
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
object ModuleCapes : ClientModule(
    "Capes",
    category = ModuleCategories.RENDER,
    hide = true,
    state = true
) {
    private val allowWithElytra by boolean("AllowWithElytra", false)

    private val mode = choices("Mode", 0) {
        arrayOf(Mode.Default, Mode.File)
    }

    private val reloadFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        ioScope.launch {
            reloadFlow.debounce(150.milliseconds).collectLatest {
                if (!running) return@collectLatest
                mode.activeMode.reloadCape()
            }
        }

        ioScope.launch {
            mode.asStateFlow().collect {
                triggerReload()
            }
        }
    }

    fun getCapeTextureId(): Identifier? {
        if (!running) return null
        return mode.activeMode.capeTextureId
    }

    fun getCapeReady(): Boolean {
        val chestStack = player.getItemBySlot(EquipmentSlot.CHEST)
        val hasElytra = chestStack.`is`(Items.ELYTRA) && chestStack.has(DataComponents.EQUIPPABLE)

        if (!allowWithElytra && hasElytra) {
            return false
        }

        return running
    }

    override suspend fun enabledEffect() {
        triggerReload()
    }

    private suspend fun triggerReload() {
        reloadFlow.emit(Unit)
    }

    private sealed class Mode(name: String) : net.ccbluex.liquidbounce.config.types.group.Mode(name) {
        final override val parent: ModeValueGroup<*>
            get() = mode

        abstract val capeTextureId: Identifier?
        abstract suspend fun reloadCape()

        object Default : Mode("Default") {
            private val textureId = LiquidBounce.identifier("capes-jmcomicfix")
            @Volatile
            override var capeTextureId: Identifier? = null
                private set

            override suspend fun reloadCape() {
                val nativeImage = withContext(Dispatchers.IO) {
                    LiquidBounce.resource("capes/JMcomicFix.png").readNativeImage()
                }

                withContext(Dispatchers.Minecraft) {
                    nativeImage.registerTexture(textureId)
                }

                capeTextureId = textureId
            }
        }

        object File : Mode("File") {
            private val image = file("Image")

            private val textureId = LiquidBounce.identifier("capes/custom-file")
            @Volatile
            override var capeTextureId: Identifier? = null
                private set

            init {
                image.asStateFlow()
                    .filter { it.isFile }
                    .let { flow ->
                        ioScope.launch {
                            flow.collectLatest { file ->
                                if (!inGame) return@collectLatest

                                val nativeImage = withContext(Dispatchers.IO) {
                                    file.readNativeImage()
                                }

                                withContext(Dispatchers.Minecraft) {
                                    nativeImage.registerTexture(textureId)
                                }

                                capeTextureId = textureId
                            }
                        }
                    }
            }

            override suspend fun reloadCape() {
                val file = image.get()
                if (!file.isFile) {
                    capeTextureId = null
                    return
                }

                val nativeImage = withContext(Dispatchers.IO) {
                    file.readNativeImage()
                }

                withContext(Dispatchers.Minecraft) {
                    nativeImage.registerTexture(textureId)
                }

                capeTextureId = textureId
            }
        }
    }
}
