package org.gitee.orryx.api.interfaces

import org.gitee.orryx.module.experience.IExperience

interface IMiscAPI {

    fun getExperience(key: String): IExperience?
}