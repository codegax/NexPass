package com.nexpass.passwordmanager.ui.navigation

/**
 * Navigation routes for NexPass
 *
 * All screen destinations are defined here for type-safe navigation
 */
object NexPassRoutes {
    const val ONBOARDING = "onboarding"
    const val UNLOCK = "unlock"
    const val VAULT_LIST = "vault_list"
    const val PASSWORD_DETAIL = "password_detail/{passwordId}"
    const val PASSWORD_CREATE = "password_create"
    const val PASSWORD_EDIT = "password_edit/{passwordId}"
    const val PASSWORD_GENERATOR = "password_generator"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val FOLDER_MANAGE = "folder_manage"
    const val TAG_MANAGE = "tag_manage"

    /**
     * Build password detail route with ID
     */
    fun passwordDetail(passwordId: String): String {
        return "password_detail/$passwordId"
    }

    /**
     * Build password edit route with ID
     */
    fun passwordEdit(passwordId: String): String {
        return "password_edit/$passwordId"
    }
}

/**
 * Navigation arguments
 */
object NexPassArguments {
    const val PASSWORD_ID = "passwordId"
}
