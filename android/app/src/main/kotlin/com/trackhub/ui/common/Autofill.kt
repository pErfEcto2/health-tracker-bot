@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.trackhub.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

/**
 * Hooks an [OutlinedTextField] (or other text input) into the platform autofill framework so
 * password managers like 1Password / Google Password Manager can fill it. Pair this with
 * `keyboardOptions` and `visualTransformation` for proper keyboard / masking behaviour.
 */
fun Modifier.autofill(
    types: List<AutofillType>,
    onFill: (String) -> Unit,
): Modifier = composed {
    val autofill = LocalAutofill.current
    val tree = LocalAutofillTree.current
    val node = remember(types) { AutofillNode(autofillTypes = types, onFill = onFill) }
    remember(node) { tree += node }

    this
        .onGloballyPositioned { node.boundingBox = it.boundsInWindow() }
        .onFocusChanged { focus ->
            autofill?.run {
                if (focus.isFocused) requestAutofillForNode(node)
                else cancelAutofillForNode(node)
            }
        }
}

@Composable
fun rememberUsernameAutofill(onFill: (String) -> Unit): Modifier =
    Modifier.autofill(types = listOf(AutofillType.Username), onFill = onFill)

@Composable
fun rememberPasswordAutofill(onFill: (String) -> Unit): Modifier =
    Modifier.autofill(types = listOf(AutofillType.Password), onFill = onFill)
