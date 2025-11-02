package org.gitee.orryx.utils

import ink.ptms.adyeshach.core.Adyeshach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.gitee.orryx.api.adapters.entity.AbstractAdyeshachEntity
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.profile.Flag
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.core.profile.SerializableFlag
import org.gitee.orryx.dao.serializer.*
import org.joml.Matrix3dc
import org.joml.Vector3dc
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

/**
 * @param isPersistence 是否持久化
 * @param timeout 存活时间(毫秒)
 * */
inline fun <reified T : Any> T.flag(isPersistence: Boolean = false, timeout: Long = 0): IFlag {
    return Flag(this, isPersistence, timeout)
}

enum class SerializableType(val key: String, val type: KClass<*>) {
    STRING("string", String::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(value as String)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString<String>(value), isPersistence, timeout)
    },
    BOOLEAN("boolean", Boolean::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(value as Boolean)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString<Boolean>(value), isPersistence, timeout)
    },
    INTEGER("integer", Int::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(value as Int)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString<Int>(value), isPersistence, timeout)
    },
    LONG("long", Long::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(value as Long)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString<Long>(value), isPersistence, timeout)
    },
    FLOAT("float", Float::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(value as Float)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString<Float>(value), isPersistence, timeout)
    },
    DOUBLE("double", Double::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(value as Double)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString<Double>(value), isPersistence, timeout)
    },
    SHORT("short", Short::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(value as Short)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString<Short>(value), isPersistence, timeout)
    },
    BYTE("byte", Byte::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(value as Byte)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString<Byte>(value), isPersistence, timeout)
    },
    CHAR("char", Char::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(value as Char)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString<Char>(value), isPersistence, timeout)
    },
    DATE("date", Date::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(value as Date)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString(DateSerializer, value), isPersistence, timeout)
    },
    INSTANT("instant", Instant::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(value as Instant)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString(InstantSerializer, value), isPersistence, timeout)
    },
    VECTOR("vector", Vector3dc::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(IVector3dcSerializer, value as Vector3dc)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString(IVector3dcSerializer, value), isPersistence, timeout)
    },
    MATRIX("matrix", Matrix3dc::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(IMatrix3dcSerializer, value as Matrix3dc)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString(IMatrix3dcSerializer, value), isPersistence, timeout)
    },
    UUID("uuid", UUID::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(UUIDSerializer, value as UUID)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag(Json.decodeFromString(UUIDSerializer, value), isPersistence, timeout)
    },
    ABSTRACT_BUKKIT_ENTITY("abstract_bukkit_entity", AbstractBukkitEntity::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString(UUIDSerializer, (value as AbstractBukkitEntity).uniqueId)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Bukkit.getEntity(Json.decodeFromString(UUIDSerializer, value))?.let { Flag(AbstractBukkitEntity(it), isPersistence, timeout) }
    },
    ABSTRACT_ADYESHACH_ENTITY("abstract_adyeshach_entity", AbstractAdyeshachEntity::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString((value as AbstractAdyeshachEntity).entityId)
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Adyeshach.api().getEntityFinder().getEntityFromEntityId(Json.decodeFromString(value))?.let { Flag(AbstractAdyeshachEntity(it), isPersistence, timeout) }
    },
    Array("array", Array::class) {
        override fun encodeToString(value: Any): encodeToString = Json.encodeToString((value as Array<*>).map {
            val type = getType(it!!)
            SerializableFlag(type.key, type.encodeToString(it), true, 0)
        })
        override fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long) = Flag((Json.decodeFromString(value) as Array<SerializableFlag>).map {
            it.toFlag()?.value
        }, isPersistence, timeout)
    };
    abstract fun encodeToString(value: Any): String
    abstract fun decodeFromString(value: String, isPersistence: Boolean, timeout: Long): Flag<*>?
}

fun IFlag.toSerializable(): SerializableFlag {
    val serializableType = getType(value)
    return SerializableFlag(serializableType.key, serializableType.encodeToString(value), isPersistence, timeout)
}

fun getType(value: Any): SerializableType {
    return when (value::class) {
        SerializableType.STRING.type -> SerializableType.STRING
        SerializableType.BOOLEAN.type -> SerializableType.BOOLEAN
        SerializableType.INTEGER.type -> SerializableType.INTEGER
        SerializableType.LONG.type -> SerializableType.LONG
        SerializableType.FLOAT.type -> SerializableType.FLOAT
        SerializableType.DOUBLE.type -> SerializableType.DOUBLE
        SerializableType.SHORT.type -> SerializableType.SHORT
        SerializableType.BYTE.type -> SerializableType.BYTE
        SerializableType.CHAR.type -> SerializableType.CHAR
        SerializableType.DATE.type -> SerializableType.DATE
        SerializableType.INSTANT.type -> SerializableType.INSTANT
        SerializableType.VECTOR.type -> SerializableType.VECTOR
        SerializableType.MATRIX.type -> SerializableType.MATRIX
        SerializableType.UUID.type -> SerializableType.UUID
        SerializableType.ABSTRACT_BUKKIT_ENTITY.type -> SerializableType.ABSTRACT_BUKKIT_ENTITY
        SerializableType.ABSTRACT_ADYESHACH_ENTITY.type -> SerializableType.ABSTRACT_ADYESHACH_ENTITY
        SerializableType.Array.type -> SerializableType.Array
        else -> error("无法序列化${value::class.simpleName}类型 请勿持久化存储")
    }
}

fun SerializableFlag.toFlag(): IFlag? {
    val serializableType = when(type) {
        SerializableType.STRING.key -> SerializableType.STRING
        SerializableType.BOOLEAN.key -> SerializableType.BOOLEAN
        SerializableType.INTEGER.key -> SerializableType.INTEGER
        SerializableType.LONG.key -> SerializableType.LONG
        SerializableType.FLOAT.key -> SerializableType.FLOAT
        SerializableType.DOUBLE.key -> SerializableType.DOUBLE
        SerializableType.SHORT.key -> SerializableType.SHORT
        SerializableType.BYTE.key -> SerializableType.BYTE
        SerializableType.CHAR.key -> SerializableType.CHAR
        SerializableType.DATE.key -> SerializableType.DATE
        SerializableType.INSTANT.key -> SerializableType.INSTANT
        SerializableType.VECTOR.key -> SerializableType.VECTOR
        SerializableType.MATRIX.key -> SerializableType.MATRIX
        SerializableType.UUID.key -> SerializableType.UUID
        SerializableType.ABSTRACT_BUKKIT_ENTITY.key -> SerializableType.ABSTRACT_BUKKIT_ENTITY
        SerializableType.ABSTRACT_ADYESHACH_ENTITY.key -> SerializableType.ABSTRACT_ADYESHACH_ENTITY
        SerializableType.Array.key -> SerializableType.Array
        else -> error("无法反序列化${type}类型")
    }
    return serializableType.decodeFromString(value, isPersistence, timeout)
}