package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.gitee.orryx.api.collider.local.ILocalCollider
import org.gitee.orryx.api.collider.local.ILocalComposite
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

open class LocalComposite<T : ITargetLocation<*>, C : ILocalCollider<T>>(
    localPosition: Vector3d,
    localRotation: Quaterniond,
    private val parent: ICoordinateConverter
) : ILocalComposite<T, C> {

    private val colliderMap = hashMapOf<String, Pair<Int, C>>()
    private val colliders = ArrayList<C>()
    private val colliderNames = ArrayList<String>()
    private val globalPosition = Vector3d()
    private val globalRotation = Quaterniond()

    /** 0 - 中心点, 1 - 旋转 */
    private val version = ShortArray(2)
    private val dirty = booleanArrayOf(true, true)
    private val child = LocalCompositeCoordinateConverter(this)

    private var disable: Boolean = false

    init {
        version[0] = (parent.positionVersion() - 1).toShort()
        version[1] = (parent.rotationVersion() - 1).toShort()
    }

    override var localPosition = localPosition
        set(value) {
            dirty[0] = true
            field = value
        }

    override var localRotation = localRotation
        set(value) {
            dirty[1] = true
            field = value
        }

    override val position: Vector3d
        get() {
            update()
            return globalPosition
        }

    override val rotation: Quaterniond
        get() {
            update()
            return globalRotation
        }

    override val converter: ICoordinateConverter
        get() = child

    override val collidersCount: Int
        get() = colliders.size

    override fun getCollider(index: Int): C {
        update()
        return colliders[index]
    }

    fun getCollider(name: String): C? {
        update()
        return colliderMap.get(name)?.second
    }

    /** 设置指定索引碰撞箱
     *
     * 仅修改碰撞箱，不会影响碰撞箱名称
     *
     * @see LocalComposite.setCollider
     */
    override fun setCollider(index: Int, collider: C) {
        colliders[index] = collider
        colliderMap.replace(colliderNames[index], index to collider)
    }

    fun setCollider(name: String, collider: C) {
        val old = colliderMap[name]
        val index: Int = old!!.first
        colliderMap.replace(name, index to collider)
        colliders[index] = collider
        colliderNames[index] = name
    }

    /** 设置指定索引碰撞箱
     *
     * 同时修改碰撞箱与名称 */
    fun setCollider(index: Int, name: String, collider: C) {
        colliders[index] = collider
        colliderMap.remove(colliderNames[index])
        colliderMap.put(name, index to collider)
        colliderNames[index] = name
    }

    /** 添加碰撞箱
     *
     * 名称为碰撞箱的索引
     *
     * @see LocalComposite.addCollider
     */
    override fun addCollider(collider: C) {
        colliders.add(collider)
        val name = colliders.size.toString()
        colliderNames.add(name)
        colliderMap.put(name, colliders.size - 1 to collider)
    }

    /** 添加碰撞箱 */
    fun addCollider(name: String, collider: C) {
        if (contains(name)) {
            setCollider(name, collider)
            return
        }

        colliderMap.put(name, colliders.size - 1 to collider)
        colliders.add(collider)
        colliderNames.add(name)
    }

    /** 添加碰撞箱
     *
     * 将碰撞箱添加到指定索引
     *
     * 尽量避免使用这个方法，因为需要对所有指定索引之后的缓存进行修改 */
    fun addCollider(index: Int, name: String, collider: C) {
        colliders.add(index, collider)
        colliderNames.add(index, name)

        for (i in index..<colliderNames.size) {
            val key = colliderNames[i]
            val pair = colliderMap[key]!!
            colliderMap.replace(key, pair.first + 1 to pair.second)
        }
    }

    /** 移除指定索引碰撞箱
     *
     * 尽量避免使用这个方法，因为需要对所有指定索引之后的缓存进行修改 */
    override fun removeCollider(index: Int) {
        val name = colliderNames[index]
        colliders.removeAt(index)
        colliderNames.removeAt(index)
        colliderMap.remove(name)

        for (i in index..<colliderNames.size) {
            val key = colliderNames[i]
            val pair = colliderMap[key]!!
            colliderMap.replace(key, pair.first - 1 to pair.second)
        }
    }

    /** 移除名称碰撞箱
     *
     * 尽量避免使用这个方法，因为需要对所有名称索引之后的缓存进行修改 */
    open fun removeCollider(name: String?) {
        val pair = colliderMap.remove(name)!!
        colliders.removeAt(pair.first)
        colliderNames.remove(name)

        for (i in pair.first..<colliderNames.size) {
            val key = colliderNames[i]
            val p = colliderMap[key]!!
            colliderMap.replace(key, p.first - 1 to p.second)
        }
    }

    fun getColliderName(index: Int): String {
        return colliderNames[index]
    }

    fun getColliderIndex(name: String): Int {
        return colliderMap.get(name)!!.first
    }

    fun contains(name: String): Boolean {
        return colliderMap.containsKey(name)
    }

    override fun setDisable(disable: Boolean) {
        this.disable = disable
    }

    override fun disable(): Boolean {
        return disable
    }

    protected fun setPositionDirty() {
        dirty[0] = true
    }

    protected fun setRotationDirty() {
        dirty[1] = true
    }

    override fun update() {
        if (parent.positionVersion() != version[0] || parent.rotationVersion() != version[1] || dirty[0] || dirty[1]) {
            dirty[0] = false
            dirty[1] = false
            version[0] = parent.positionVersion()
            version[1] = parent.rotationVersion()
            child.update()
            val position = parent.position
            val rotation = parent.rotation
            rotation.transform(localPosition, globalPosition).add(position)
            rotation.mul(localRotation, globalRotation)
        }
    }
}