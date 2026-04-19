package com.peihua.genui.catalog.basic_catalog_widgets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.StarHalf
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import com.peihua.genui.model.A2uiSchemas
import com.peihua.genui.model.CatalogItem
import com.peihua.genui.primitives.JsonMap
import com.peihua.genui.widgets.BoundString
import com.peihua.json.schema.S
import kotlin.collections.List
object Icon {
    val _schema = S.obj(
        description = "Displays an icon from a set of available icons.",
        properties = mapOf(
            "name" to A2uiSchemas.stringReference(
                description = """The name of the icon to display. This can be a literal string or a reference to a value in the data model ("path', e.g. '/icon/name').""",
                enumValues = AvailableIcons.allAvailable,
            ),
        ),
        required = listOf("name"),
    )

    /**
     * A standard icon.
     *
     * ### Parameters:
     *
     * - `name`: The name of the icon to display.
     */
    val icon = CatalogItem(
        name = "Icon",
        schema = _schema,
        widgetBuilder = { itemContext ->
            return@CatalogItem BoundString(
                dataContext = itemContext.dataContext,
                value = (itemContext.data as JsonMap)["name"],
                builder = { currentValue ->
                    val iconName = currentValue ?: ""
                    val icon = AvailableIcons.fromName(iconName)?.iconData ?: Icons.Default.BrokenImage
                    Icon(icon, contentDescription = iconName)
                },
            )
        },
        exampleData = listOf(
            {
                """
                [
                    {
                        "id": "root",
                        "component": "Icon",
                        "name": "add"
                    }
                ]
               """
            },
        )
    );

}

enum class AvailableIcons(val iconName: String, val iconData: ImageVector) {
    /**
     * The name of the icon to display.
     */
    ACCOUNT_CIRCLE("accountCircle", Icons.Default.AccountCircle),
    ADD("add", Icons.Default.Add),
    ARROW_BACK("arrowBack", Icons.Default.ArrowBackIosNew),
    ARROW_FORWARD("arrowForward", Icons.AutoMirrored.Default.ArrowForwardIos),
    ATTACH_FILE("attachFile", Icons.Default.AttachFile),
    CALENDAR_TODAY("calendarToday", Icons.Default.CalendarToday),
    CALL("call", Icons.Default.Call),
    CAMERA("camera", Icons.Default.CameraAlt),
    CHECK("check", Icons.Default.Check),
    CLOSE("close", Icons.Default.Close),
    DELETE("delete", Icons.Default.Delete),
    DOWNLOAD("download", Icons.Default.Download),
    EDIT("edit", Icons.Default.Edit),
    ERROR("error", Icons.Default.Error),
    EVENT("event", Icons.Default.Event),
    FAVORITE("favorite", Icons.Default.Favorite),
    FAVORITE_OFF("favoriteOff", Icons.Default.FavoriteBorder),
    FOLDER("folder", Icons.Default.Folder),
    HELP("help", Icons.AutoMirrored.Default.Help),
    HOME("home", Icons.Default.Home),
    INFO("info", Icons.Outlined.Info),
    LOCATION_ON("locationOn", Icons.Default.LocationOn),
    LOCK("lock", Icons.Outlined.Lock),
    LOCK_OPEN("lockOpen", Icons.Outlined.LockOpen),
    MAIL("mail", Icons.Default.MailOutline),
    MENU("menu", Icons.Default.Menu),
    MORE_HORIZ("moreHoriz", Icons.Default.MoreHoriz),
    MORE_VERT("moreVert", Icons.Default.MoreVert),
    NOTIFICATIONS("notifications", Icons.Default.Notifications),
    NOTIFICATIONS_OFF("notificationsOff", Icons.Default.NotificationsNone),
    PAYMENT("payment", Icons.Default.Payment),
    PERSON("person", Icons.Default.Person),
    PHONE("phone", Icons.Default.Phone),
    PHOTO("photo", Icons.Default.Photo),
    PRINT("print", Icons.Default.Print),
    REFRESH("refresh", Icons.Default.Refresh),
    SEARCH("search", Icons.Default.Search),
    SEND("send", Icons.AutoMirrored.Default.Send),
    SETTINGS("settings", Icons.Default.Settings),
    SHARE("share", Icons.Default.Share),
    SHOPPING_CART("shoppingCart", Icons.Default.ShoppingCart),
    STAR("star", Icons.Default.Star),
    STAR_HALF("starHalf", Icons.AutoMirrored.Outlined.StarHalf),
    STAR_OFF("starOff", Icons.Default.StarOutline),
    UPLOAD("upload", Icons.Default.Upload),
    VISIBILITY("visibility", Icons.Default.Visibility),
    VISIBILITY_OFF("visibilityOff", Icons.Default.VisibilityOff),
    WARNING("warning", Icons.Default.Warning);

    companion object {
        val allAvailable: List<String>
            get() = AvailableIcons.entries.map { it.iconName }.toList();

        fun fromName(name: String): AvailableIcons? {
            for (iconName in AvailableIcons.entries) {
                if (iconName.name == name) {
                    return iconName;
                }
            }
            return null;
        }
    }
}