package ink.ptms.adyeshach.impl

import ink.ptms.adyeshach.core.*
import ink.ptms.adyeshach.core.entity.manager.ManagerType
import ink.ptms.adyeshach.impl.entity.manager.BaseManager
import ink.ptms.adyeshach.impl.entity.manager.BasePlayerManager
import ink.ptms.adyeshach.impl.entity.manager.DefaultManager
import ink.ptms.adyeshach.impl.entity.manager.DefaultPlayerManager
import org.bukkit.entity.Player
import taboolib.common.platform.PlatformFactory
import taboolib.platform.util.removeMeta
import taboolib.platform.util.setMeta
import java.util.concurrent.ConcurrentHashMap

/**
 * Adyeshach
 * ink.ptms.adyeshach.impl.nms.DefaultAdyeshachAPI
 *
 * @author 坏黑
 * @since 2022/6/18 16:20
 */
class DefaultAdyeshachAPI : AdyeshachAPI {

    /** 单位检索接口 **/
    var localEntityFinder = PlatformFactory.getAPI<AdyeshachEntityFinder>()

    /** 单位序列化接口 **/
    var localEntitySerializer = PlatformFactory.getAPI<AdyeshachEntitySerializer>()

    /** 单位类型管理接口 **/
    var localEntityTypeHelper = PlatformFactory.getAPI<AdyeshachEntityTypeRegistry>()

    /** 单位元数据管理接口 **/
    var localEntityMetadataHandler = PlatformFactory.getAPI<AdyeshachEntityMetadataRegistry>()

    /** 单位控制器管理接口 **/
    var localEntityControllerHandler = PlatformFactory.getAPI<AdyeshachEntityControllerRegistry>()

    /** 全系接口 **/
    var localHologramHandler = PlatformFactory.getAPI<AdyeshachHologramHandler>()

    /** 脚本接口 **/
    var localKetherHandler = PlatformFactory.getAPI<AdyeshachKetherHandler>()

    /** 服务端逆向操作工具 **/
    var localMinecraftAPI = PlatformFactory.getAPI<AdyeshachMinecraftAPI>()

    /** 网络工具 **/
    var localNetworkAPI = PlatformFactory.getAPI<AdyeshachNetworkAPI>()

    /** 语言文件接口 **/
    var localLanguage = PlatformFactory.getAPI<AdyeshachLanguage>()

    /** 公共单位管理器 **/
    var localPublicEntityManager = DefaultManager()

    /** 公共单位管理器（孤立） **/
    var localPublicEntityManagerIsolated = DefaultManager()

    /** 公共单位管理器（临时） **/
    var localPublicEntityManagerTemporary = DefaultManager()

    override fun setupEntityManager(player: Player) {
        if (!player.hasMetadata("adyeshach_setup")) {
            // 设置标签避免重复执行
            player.setMeta("adyeshach_setup", true)
            // 公共管理器
            getPublicEntityManager(ManagerType.PERSISTENT).getEntities { it.visibleAfterLoaded }.forEach {
                it.viewPlayers.viewers += player.name
            }
            getPublicEntityManager(ManagerType.TEMPORARY).getEntities { it.visibleAfterLoaded }.forEach {
                it.viewPlayers.viewers += player.name
            }
            // 私有管理器
            getPrivateEntityManager(player).onEnable()
        }
    }

    override fun releaseEntityManager(player: Player) {
        if (player.hasMetadata("adyeshach_setup")) {
            player.removeMeta("adyeshach_setup")
            // 公共管理器
            getPublicEntityManager(ManagerType.PERSISTENT).getEntities { it.visibleAfterLoaded }.forEach {
                it.removeViewer(player)
            }
            getPublicEntityManager(ManagerType.TEMPORARY).getEntities { it.visibleAfterLoaded }.forEach {
                it.removeViewer(player)
            }
            // 私有管理器
            var privateManager = getPrivateEntityManager(player, ManagerType.PERSISTENT)
            privateManager.onDisable()
            privateManager.onSave()
            privateManager = getPrivateEntityManager(player, ManagerType.TEMPORARY)
            privateManager.onDisable()
            // 移除管理器
            playerEntityManagerMap.remove(player.name)
            playerEntityTemporaryManagerMap.remove(player.name)
        }
    }

    override fun refreshEntityManager(player: Player) {
        // 对范围内可视且在观察者列表的实体进行刷新
        localEntityFinder.getVisibleEntities(player).forEach { it.visible(player, true) }
    }

    override fun getPublicEntityManager(type: ManagerType): BaseManager {
        return when (type) {
            ManagerType.PERSISTENT -> localPublicEntityManager
            ManagerType.TEMPORARY -> localPublicEntityManagerTemporary
            ManagerType.ISOLATED -> localPublicEntityManagerIsolated
        }
    }

    override fun getPrivateEntityManager(player: Player, type: ManagerType): BaseManager {
        if (type == ManagerType.ISOLATED) {
            return BasePlayerManager(player)
        }
        val map = if (type == ManagerType.TEMPORARY) playerEntityTemporaryManagerMap else playerEntityManagerMap
        return if (map.containsKey(player.name)) {
            map[player.name]!!
        } else {
            val manager = DefaultPlayerManager(player)
            map[player.name] = manager
            manager
        }
    }

    override fun getEntityFinder(): AdyeshachEntityFinder {
        return localEntityFinder
    }

    override fun getEntitySerializer(): AdyeshachEntitySerializer {
        return localEntitySerializer
    }

    override fun getEntityTypeRegistry(): AdyeshachEntityTypeRegistry {
        return localEntityTypeHelper
    }

    override fun getEntityMetadataRegistry(): AdyeshachEntityMetadataRegistry {
        return localEntityMetadataHandler
    }

    override fun getEntityControllerRegistry(): AdyeshachEntityControllerRegistry {
        return localEntityControllerHandler
    }

    override fun getHologramHandler(): AdyeshachHologramHandler {
        return localHologramHandler
    }

    override fun getKetherHandler(): AdyeshachKetherHandler {
        return localKetherHandler
    }

    override fun getMinecraftAPI(): AdyeshachMinecraftAPI {
        return localMinecraftAPI
    }

    override fun getNetworkAPI(): AdyeshachNetworkAPI {
        return localNetworkAPI
    }

    override fun getLanguage(): AdyeshachLanguage {
        return localLanguage
    }

    companion object {

        val playerEntityManagerMap = ConcurrentHashMap<String, DefaultPlayerManager>()
        val playerEntityTemporaryManagerMap = ConcurrentHashMap<String, DefaultPlayerManager>()
    }
}