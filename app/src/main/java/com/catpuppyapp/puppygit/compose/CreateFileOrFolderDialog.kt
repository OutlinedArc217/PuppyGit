package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.constants.Cons
import com.catpuppyapp.puppygit.play.pro.R


@Deprecated("use `CreateFileOrFolderDialog2` instead")
@Composable
fun CreateFileOrFolderDialog(
    cancelBtnText: String = stringResource(R.string.cancel),
    okBtnText: String = stringResource(R.string.ok),
    cancelTextColor: Color = Color.Unspecified,
    okTextColor: Color = Color.Unspecified,
    errMsg: MutableState<String>,
    fileName:MutableState<String>,
    fileTypeOptions:List<String>,
    selectedFileTypeOption:MutableIntState,
    onCancel: () -> Unit,
    onOk: (String, Int) -> Boolean,
) {
    val doCreate = doCreate@{
        val fileType = if (selectedFileTypeOption.intValue == 0) Cons.fileTypeFile else Cons.fileTypeFolder
        //执行用户传入的callback
        val createSuccess = onOk(fileName.value, fileType)
        if(createSuccess) {
            //关闭对话框
            onCancel()
        }

    }

    val hasErr = {
        errMsg.value.isNotEmpty()
    }

    AlertDialog(
        title = {
            DialogTitle(stringResource(R.string.create))
        },
        text = {
            ScrollableColumn {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            if(event.key == Key.Enter) {
                                doCreate()
                                true
                            }else {
                                false
                            }
                        }
                    ,

                    value = fileName.value,
                    singleLine = true,
                    onValueChange = {
                        //一修改就清空错误信息，然后点创建的时候会再检测，若有错误会再设置上
                        errMsg.value = ""

                        fileName.value = it
                    },
                    isError = hasErr(),
                    supportingText = {
                        if (hasErr()) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = errMsg.value,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    trailingIcon = {
                        if (hasErr()) {
                            Icon(imageVector= Icons.Filled.Error,
                                contentDescription=errMsg.value,
                                tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    label = {
                        Text(stringResource(R.string.file_or_folder_name))
                    },
                    placeholder = {
//                    Text(stringResource(R.string.file_name_placeholder))
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        doCreate()
                    })
                )

                Spacer(modifier = Modifier.padding(15.dp))
                SingleSelection(
                    itemList = fileTypeOptions,
                    selected = {idx, item -> selectedFileTypeOption.intValue == idx},
                    text = {idx, item -> item},
                    onClick = {idx, item -> selectedFileTypeOption.intValue = idx}
                )

            }
        },
        //点击弹框外区域的时候触发此方法，一般设为和OnCancel一样的行为即可
        onDismissRequest = onCancel,
        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(
                    text = cancelBtnText,
                    color = cancelTextColor,
                )
            }
        },
        confirmButton = {
            val enabled = fileName.value.isNotEmpty() && !hasErr()

            TextButton(
                enabled = enabled,
                onClick = {
                    doCreate()
                },
            ) {
                Text(
                    text = okBtnText,
                    color = if(enabled) okTextColor else Color.Unspecified,
                )
            }
        },

    )

}
