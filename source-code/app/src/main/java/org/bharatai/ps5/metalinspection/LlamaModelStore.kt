package org.bharatai.ps5.metalinspection

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/** Copies and verifies user-selected Llama files before passing them to native code. */
internal object LlamaModelStore {
    const val MODEL_BYTES = 2_471_922_052L
    const val TOKENIZER_BYTES = 2_183_982L
    private const val MODEL_SHA256 = "57fe8710296f61f6be07c21f68c6917d18355b521be4242a41598a7238755396"
    private const val TOKENIZER_SHA256 = "82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55"

    fun isValidModel(file: File): Boolean = file.isFile && file.length() == MODEL_BYTES

    fun isValidTokenizer(file: File): Boolean = file.isFile && file.length() == TOKENIZER_BYTES

    /** Run on an IO dispatcher; this streams the 2.47 GB model rather than reading it into memory. */
    fun verifyPair(model: File, tokenizer: File) {
        check(isValidModel(model)) {
            "Saved model is incomplete (${model.length()} of $MODEL_BYTES bytes). Choose the full .pte file again."
        }
        check(isValidTokenizer(tokenizer)) {
            "Saved tokenizer is incomplete (${tokenizer.length()} of $TOKENIZER_BYTES bytes). Choose tokenizer.model again."
        }
        check(sha256(model) == MODEL_SHA256) {
            "Saved model is damaged or is a different version. Choose the original complete model file."
        }
        check(sha256(tokenizer) == TOKENIZER_SHA256) {
            "Tokenizer does not match this Llama model. Choose its matching tokenizer.model file."
        }
    }

    suspend fun copyIntoPrivateStorage(context: Context, uri: Uri, fileName: String): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "llama").apply { mkdirs() }
            val destination = File(dir, fileName)
            val partial = File(dir, "$fileName.part")
            val (expectedBytes, expectedHash) = when (fileName) {
                "Llama-3.2-1B-Instruct.pte" -> MODEL_BYTES to MODEL_SHA256
                "tokenizer.model" -> TOKENIZER_BYTES to TOKENIZER_SHA256
                else -> error("Unsupported Llama file: $fileName")
            }

            partial.delete()
            val digest = MessageDigest.getInstance("SHA-256")
            var copiedBytes = 0L
            try {
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Could not open selected file" }
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(1024 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            copiedBytes += count
                        }
                        output.fd.sync()
                    }
                }
                check(copiedBytes == expectedBytes && partial.length() == expectedBytes) {
                    "Selected file is incomplete ($copiedBytes of $expectedBytes bytes copied). Choose the complete downloaded file."
                }
                check(digest.digest().toHex() == expectedHash) {
                    "Selected file contents do not match this Llama model. Choose the original complete file."
                }

                val backup = File(dir, "$fileName.previous")
                backup.delete()
                val hadDestination = destination.exists()
                if (hadDestination) check(destination.renameTo(backup)) { "Could not safely replace the existing file" }
                if (!partial.renameTo(destination)) {
                    if (hadDestination) backup.renameTo(destination)
                    error("Could not finalize the verified model file")
                }
                backup.delete()
                destination
            } catch (error: Throwable) {
                partial.delete()
                throw error
            }
        }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
