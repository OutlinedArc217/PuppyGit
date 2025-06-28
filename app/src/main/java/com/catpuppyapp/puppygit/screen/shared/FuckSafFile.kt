package com.catpuppyapp.puppygit.screen.shared

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.catpuppyapp.puppygit.etc.PathType
import com.catpuppyapp.puppygit.utils.FsUtils
import com.catpuppyapp.puppygit.utils.MyLog
import com.catpuppyapp.puppygit.utils.doJobThenOffLoading
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException


private const val TAG = "FuckSafFile"

/**
 * fuck the saf api, saf should full-compatible with File api and support jni, but it didn't, so it just shit!
 *
 * note: the saf api `canRead()` `isFile()` and maybe others, they are maybe return incorrect result!!
 *   e.g. a file can read, but the `canRead()` may return false.
 *
 * @param context if only use File, no need the content uri, then can pass null to context
 *
 */
class FuckSafFile(val context: Context?, val path: FilePath) {
    companion object {
        fun fromFile(f:File):FuckSafFile {
            return FuckSafFile(null, FilePath(f.canonicalPath))
        }
    }

    var isSaf:Boolean = false

    var file:File? = null

    var safFile:DocumentFile? = null
    var safUri:Uri? = null

    //注：saf判断isFile并不准确，有可能目标是文件，但返回假
    val isFile:Boolean
        get() = (if(isSaf) safFile?.isFile else file?.isFile) == true

    val isDirectory:Boolean
        get() = (if(isSaf) safFile?.isDirectory else file?.isDirectory) == true

    val name:String
        get() = (if(isSaf) safFile?.name else file?.name) ?: ""

    val canonicalPath:String
        get() = (if(isSaf) safUri?.toString() else file?.canonicalPath) ?: ""


    init {
        val ioPath = path.ioPath
        val pathType = path.ioPathType

        if(pathType == PathType.ABSOLUTE) {
            initFile(ioPath)
        }else if(pathType == PathType.CONTENT_URI) {
            initDocFile(ioPath)
        }else if(pathType == PathType.FILE_URI) {
            val realPath = ioPath.removePrefix(FsUtils.fileUriPathPrefix)
            initFile(realPath)
        }
    }

    /**
     * 最后修改时间，单位毫秒
     */
    fun lastModified():Long {
        return (if (isSaf) safFile?.lastModified() else file?.lastModified()) ?: 0L
    }

    /**
     * android doesn't support get creation time yet
     */
    fun creationTime():Long {
        return lastModified()
    }

    fun length():Long {
        return (if (isSaf) safFile?.length() else file?.length()) ?: 0L
    }

    private fun initFile(path:String) {
        isSaf = false
        file = File(path)
    }

    private fun initDocFile(path:String) {
        isSaf = true
        val currentUri = Uri.parse(path)
        safUri = currentUri
        safFile = FsUtils.getDocumentFileFromUri(context!!, currentUri)
    }

    fun inputStream():InputStream {
        return if(isSaf) context!!.contentResolver.openInputStream(safUri!!)!! else file!!.inputStream()
    }

    fun outputStream():OutputStream {
        return if(isSaf) context!!.contentResolver.openOutputStream(safUri!!)!! else file!!.outputStream()
    }

    fun bufferedReader():BufferedReader {
        return inputStream().bufferedReader()
    }

    fun bufferedWriter(): BufferedWriter {
        return outputStream().bufferedWriter()
    }

    fun exists():Boolean {
        return (if(isSaf) safFile?.exists() else file?.exists()) == true
    }

    fun renameFileTo(newPath:String):Boolean {
        return renameFileTo(File(newPath))
    }

    fun renameFileTo(newFile:File):Boolean {
        return file?.renameTo(newFile) == true
    }

    fun renameSafFile(newName:String):Boolean {
        return safFile?.renameTo(newName) == true
    }

    fun delete():Boolean {
        return (if(isSaf) safFile?.delete() else file?.delete()) == true
    }

    fun renameTo(newFuckSafFile:FuckSafFile):Boolean {
        inputStream().copyTo(newFuckSafFile.outputStream())

        return delete()
    }

    fun copyTo(target:OutputStream) {
        FsUtils.copy(inputStream(), target)
    }

    fun canRead():Boolean {
        // 注：saf的 canRead并不准，有时候明明能读，告诉你不能
        return (if(isSaf) safFile?.canRead() else file?.canRead()) == true
    }

    override fun toString(): String {
        return canonicalPath
    }

    fun createChangeListener(intervalInMillSec:Long, onChange:()->Unit):Job? {
        if(path.ioPathType == PathType.INVALID) {
            return null
        }

        return doJobThenOffLoading {
            try {
                var oldFileLen = length()
                var oldFileModified = lastModified()
                while (true) {
                    delay(intervalInMillSec)

                    val newFileLen = length()
                    val newFileModified = lastModified()
                    if (oldFileLen != newFileLen || oldFileModified != newFileModified) {
                        oldFileLen = newFileLen
                        oldFileModified = newFileModified

                        onChange()
                    }
                }
            } catch (_: CancellationException) {
                // task may be canceled normally, just ignore

            } catch (e: Exception) {
                MyLog.d(TAG, "listen change of file err: filePath='${path.ioPath}', err=${e.stackTraceToString()}")
            }
        }
    }


    // due to the saf apis are unreliable, so need a simple test to check the file is actually readable and exist or not
    fun isActuallyReadable(): Boolean {
        return try {
            bufferedReader().use { it.read() }
            true
        }catch(_: Exception) {
            false
        }
    }

}
