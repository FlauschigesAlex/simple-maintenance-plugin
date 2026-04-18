package at.flauschigesalex.maintenance.utils

import java.util.Locale
import java.util.ResourceBundle

internal object Translation {
    fun translate(key: String, locale: Locale): String {
        val bundle = ResourceBundle.getBundle(
            "maintenance",
            locale,
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)
        )
        return bundle.getString(key)
    }
}