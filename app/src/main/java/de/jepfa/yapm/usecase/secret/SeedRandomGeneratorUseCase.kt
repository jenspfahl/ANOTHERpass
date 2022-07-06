package de.jepfa.yapm.usecase.secret

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.usecase.UseCase
import de.jepfa.yapm.usecase.UseCaseOutput
import de.jepfa.yapm.util.toHex
import de.jepfa.yapm.util.toastText
import java.nio.ByteBuffer

object SeedRandomGeneratorUseCase: UseCase<Bitmap, String, BaseActivity> {

    val REQUEST_IMAGE_CAPTURE = 18353

    override suspend fun execute(img: Bitmap, activity: BaseActivity): UseCaseOutput<String> {
        val seedFromThumbnailImage = extractSeedFromImage(img, activity)
        if (seedFromThumbnailImage != null) {
            SecretService.setUserSeed(seedFromThumbnailImage, activity)
            val seedToPresent = seedFromThumbnailImage.data.copyOf(8).toHex() + "********"
            return UseCaseOutput(seedToPresent)
        }
        else {
            return UseCaseOutput(false, "")
        }
    }

    fun openDialog(baseActivity: BaseActivity, fragmentForResult: BaseFragment? = null) {

        if (PreferenceService.isPresent(PreferenceService.DATA_ENCRYPTED_SEED, baseActivity)) {

            AlertDialog.Builder(baseActivity)
                .setTitle(R.string.title_add_user_seed)
                .setMessage(R.string.message_confirm_add_user_seed)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    openDialogAndTakePicture(baseActivity, fragmentForResult)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        else {
            openDialogAndTakePicture(baseActivity, fragmentForResult)
        }
    }

    private fun openDialogAndTakePicture(baseActivity: BaseActivity, fragmentForResult: BaseFragment? = null) {
        AlertDialog.Builder(baseActivity)
            .setTitle(R.string.title_add_user_seed)
            .setMessage(R.string.message_add_user_seed)
            .setPositiveButton(android.R.string.ok) { _, _ ->

                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                try {
                    if (fragmentForResult != null) {
                        fragmentForResult.startActivityForResult(
                            takePictureIntent,
                            REQUEST_IMAGE_CAPTURE
                        )
                    }
                    else {
                        baseActivity.startActivityForResult(
                            takePictureIntent,
                            REQUEST_IMAGE_CAPTURE
                        )
                    }
                } catch (e: ActivityNotFoundException) {
                    toastText(baseActivity, R.string.error_add_user_seed)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun extractSeedFromImage(imageBitmap: Bitmap, context: Context): Key? {

        Log.i("SEED byte count", imageBitmap.byteCount.toString())
        Log.i("SEED x", imageBitmap.width.toString())
        Log.i("SEED y", imageBitmap.height.toString())

        if (imageBitmap.width < 32 || imageBitmap.height < 32) {
            Log.i("SEED", "thumbnail image too small to extract a random seed")
            toastText(context, R.string.error_picture_too_small_for_user_seed)
            return null
        }


        val size: Int = imageBitmap.rowBytes * imageBitmap.height
        val byteBuffer = ByteBuffer.allocate(size)
        imageBitmap.copyPixelsToBuffer(byteBuffer)
        val byteArray = byteBuffer.array()
        imageBitmap.recycle()

        return SecretService.fastHash(byteArray, SaltService.getSalt(context))
    }

}