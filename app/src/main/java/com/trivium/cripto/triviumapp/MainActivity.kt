package com.trivium.cripto.triviumapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private val RESULT = 1

    var btnPickImage: Button? = null
    var ivGallery: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.btnPickImage = findViewById<Button>(R.id.btnPickImage)
        this.btnPickImage?.setOnClickListener {
            val intent = Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(intent, RESULT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESULT && resultCode == Activity.RESULT_OK && null != data) {
            val exifData = data?.getData()!!
            val ins: InputStream? = getContentResolver()?.openInputStream(exifData);
            val img: Bitmap? = BitmapFactory.decodeStream(ins);

            if (img != null) {
                (findViewById(R.id.ivGallery) as ImageView).setImageBitmap(pictureTurn(img, exifData));
            }
        }
    }

    private fun pictureTurn(img : Bitmap, uri : Uri) : Bitmap {
        val columns = arrayOf(MediaStore.MediaColumns.DATA) // Creo que esta linea rompe...
        val c = getContentResolver()?.query(uri, columns, null, null, null)
        if (c == null) {
            Log.d("", "Could not get cursor");
            return img;
        }

        c.moveToFirst()
        val str = c.getString(0)
        if (str == null) {
            Log.d("", "Could not get exif");
            return img;
        }

        val exifInterface = ExifInterface(c.getString(0)!!) // Aca se rompe...
        val exifR : Int = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        val orientation : Float =
                when (exifR) {
                    ExifInterface.ORIENTATION_ROTATE_90 ->  90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }

        val mat : Matrix? = Matrix()
        mat?.postRotate(orientation)
        return Bitmap.createBitmap(img as Bitmap, 0, 0, img?.getWidth() as Int,
                img?.getHeight() as Int, mat, true)
    }
}
