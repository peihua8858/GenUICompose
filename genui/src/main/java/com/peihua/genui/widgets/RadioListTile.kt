package com.peihua.genui.widgets

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun <T> RadioListTile(
    modifier: Modifier = Modifier,
    checked: Boolean,
    value: T,
    groupValue: T? = null,
    dense: Boolean = false,
    indication: Indication? = LocalIndication.current,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    controlAffinity: ListTileControlAffinity = ListTileControlAffinity.leading,
    onChanged: (T?) -> Unit,
    title: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(
                indication = indication,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onChanged(value)
            },
        verticalAlignment = verticalAlignment,
        horizontalArrangement = horizontalArrangement,
    ) {
        if (controlAffinity == ListTileControlAffinity.leading) {
            RadioButton(selected = checked, onClick = { onChanged(value) })
            title()
        } else {
            title()
            RadioButton(selected = checked, onClick = { onChanged(value) })
        }
    }
}