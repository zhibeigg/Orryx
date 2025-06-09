package org.gitee.orryx.api.collider.local

import org.gitee.orryx.api.collider.ICollider
import org.gitee.orryx.core.targets.ITargetLocation

interface ILocalCollider<T: ITargetLocation<*>> : ICollider<T>