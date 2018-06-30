package com.trivium.cripto.triviumapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import java.io.InputStream
import java.util.*
import android.support.v4.app.NotificationCompat.getExtras
import java.io.ByteArrayOutputStream
import android.R.attr.bitmap
import java.nio.ByteBuffer


/* Logica basada en los siguientes links:

   - http://learn2codex.com/2017/12/29/select-image-from-gallery-and-display-in-imageview-android/
   - https://github.com/dictav/AndroidSample-kotlin/blob/master/AndroidSample/src/main/kotlin/com/dictav/androidsample/PickphotoActivity.kt
   - https://www.techotopia.com/index.php/Kotlin_-_Making_Runtime_Permission_Requests_in_Android

   */
class MainActivity : AppCompatActivity() {

    private val TAG = "PermissionDemo"
    private val READ_REQUEST_CODE = 101
    private val RESULT = 1

    var btnPickImage: Button? = null
    var ivGallery: ImageView? = null

    var selectedImage: Bitmap? = null

    var isEncrypted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupPermissions()

        this.btnPickImage = findViewById<Button>(R.id.btnPickImage)
        this.btnPickImage?.setOnClickListener {
            val intent = Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(intent, RESULT)
        }

        findViewById<Button>(R.id.btnEncryptImage).setOnClickListener {
            encrypt()
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
                findViewById<Button>(R.id.btnEncryptImage).visibility = View.VISIBLE
            }
        }
    }

    // Permisos para acceder a la galeria de imagenes.
    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission for gallery denied")
            makeRequest()
        } else {
            Log.i(TAG, "Permission ok")
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_REQUEST_CODE)
    }

    // Seleccion de imagen de la galeria.
    private fun pictureTurn(img : Bitmap, uri : Uri) : Bitmap {
        val columns = arrayOf(MediaStore.MediaColumns.DATA)
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

        val exifInterface = ExifInterface(c.getString(0)!!)
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

        val image = Bitmap.createBitmap(
                img as Bitmap,
                0,
                0,
                img?.getWidth() as Int,
                img?.getHeight() as Int,
                mat,
                true
        )

        selectedImage = image

        return image
    }

    // Encriptacion de la imagen seleccionada.
    fun encrypt() {
        Log.i(TAG, "Encriptar imagen")

        // Generamos los parametros Random iv y key
        val iv = ByteArray(80)
        Random().nextBytes(iv)
        val key = ByteArray(10)
        Random().nextBytes(key)

        // Inicializamos el encriptador de Trivium
        val triviumImageEncrypter = TriviumImageEncrypter(key, iv)

        // Obtenemos un byte array a partir de la imagen seleccionada
        val bmp = selectedImage
        val stream = ByteArrayOutputStream()
        bmp!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        bmp.recycle()

        // Generamos la imagen para encriptar
        val imageLoader = ImageLoader()
        val bytesHeader = imageLoader.getBytesHeader(byteArray)
        val bytesBody = imageLoader.getBytesBody(byteArray)
        val image = Image(byteArray, bytesHeader, bytesBody)

        // Obtenemos la imagen encriptada en forma de bytes
        val encryptedBytes = triviumImageEncrypter.encrypt(image)

        // Convertimos los bytes en imagen

        val width = selectedImage!!.getWidth() / 2
        val height = selectedImage!!.getHeight() / 2
        val newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val buffer = ByteBuffer.wrap(encryptedBytes)

        val bitmapSize = newBmp.getByteCount()
        val bufferCapacity = buffer.capacity()

        Log.i("", "Bitmap size = " + bitmapSize);
        Log.i("", "Buffer size = " + bufferCapacity);

        buffer.rewind()

        newBmp.copyPixelsFromBuffer(buffer)

        // Seteamos la imagen encriptada
        (findViewById(R.id.ivGallery) as ImageView).setImageBitmap(newBmp)
    }
}
