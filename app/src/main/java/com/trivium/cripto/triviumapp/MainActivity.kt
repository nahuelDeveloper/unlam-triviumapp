package com.trivium.cripto.triviumapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
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
import java.io.ByteArrayOutputStream
import android.widget.Toast
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset


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

    var originalImage: Bitmap? = null
    var selectedImage: Bitmap? = null
    var encryptedImage: Bitmap? = null
    //var decryptedImage: Bitmap? = null

    var isEncrypted: Boolean = false

    var triviumImageEncrypter: TriviumImageEncrypter? = null

    var shouldSetSelectedImage: Boolean = false

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

        findViewById<Button>(R.id.btnDecryptImage).setOnClickListener {
            decrypt()
        }

        initializeTriviumEncrypter()

        // Pruebas para cargar imagenes
/*
        try {
            // get input stream
            val ims = assets.open("test_image.bmp")
            // load image as Drawable
            val d = Drawable.createFromStream(ims, null)
            // set image to ImageView
            findViewById<ImageView>(R.id.ivGallery).setImageDrawable(d)
        }
        catch(ex: IOException) {
            Log.d("","Image not found")
            return
        }
*/

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESULT && resultCode == Activity.RESULT_OK && null != data) {
            val exifData = data.data!!
            val ins: InputStream? = contentResolver?.openInputStream(exifData)
            val img: Bitmap? = BitmapFactory.decodeStream(ins)

            if (img != null) {
                (findViewById<ImageView>(R.id.ivGallery)).setImageBitmap(pictureTurn(img, exifData))
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
        val c = contentResolver?.query(uri, columns, null, null, null)
        if (c == null) {
            Log.d("", "Could not get cursor")
            return img
        }

        c.moveToFirst()
        val str = c.getString(0)
        if (str == null) {
            Log.d("", "Could not get exif")
            return img
        }

        val exifInterface = ExifInterface(c.getString(0)!!)
        val exifR : Int = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
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
                img,
                0,
                0,
                img.width,
                img.height,
                mat,
                true
        )

        selectedImage = image
        originalImage = image.copy(Bitmap.Config.RGB_565, false)

        return image
    }

    fun initializeTriviumEncrypter() {
        // Generamos los parametros Random iv y key
/*
        val iv = ByteArray(80)
        Random().nextBytes(iv)
        val key = ByteArray(10)
        Random().nextBytes(key)
*/
        val key = "1160398827".toByteArray(Charset.defaultCharset())
        val iv = "1161688572".toByteArray(Charset.defaultCharset())

        // Inicializamos el encriptador de Trivium
        triviumImageEncrypter = TriviumImageEncrypter(key, iv)
        triviumImageEncrypter!!.initialize()
    }

    // Encriptacion de la imagen seleccionada.
    fun encrypt() {
        Log.i(TAG, "Encriptar imagen")

        // Obtenemos un byte array a partir de la imagen seleccionada
        val bmp = selectedImage

        // Conversion de Bitmap a ByteArray con compresion.
/*
        val stream = ByteArrayOutputStream()
        bmp!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        bmp.recycle()
*/

        // Conversion de Bitmap a ByteArray sin compresion.
        var byBuffer = ByteBuffer.allocate(bmp!!.byteCount)
        bmp.copyPixelsToBuffer(byBuffer)
        byBuffer.rewind()
        val byteArray = byBuffer.array()

        // Generamos la imagen para encriptar
        val imageLoader = ImageLoader()
        val bytesHeader = imageLoader.getBytesHeader(byteArray)
        val bytesBody = imageLoader.getBytesBody(byteArray)
        val image = Image(byteArray, bytesHeader, bytesBody)

        // Obtenemos la imagen encriptada en forma de bytes
        val encryptedBytes = triviumImageEncrypter!!.encrypt(image)

        // Convertimos los bytes en imagen
/*
        val width = selectedImage!!.width / 4
        val height = selectedImage!!.height / 4
        encryptedImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
*/

        val byteArrayLength = encryptedBytes.count()
        val bitmap = BitmapFactory.decodeByteArray(encryptedBytes, 0, byteArrayLength)

        val buffer = ByteBuffer.wrap(encryptedBytes)
        //val buffer = ByteBuffer.allocate(encryptedImage!!.getByteCount())

        val bitmapSize = encryptedImage!!.byteCount
        val bufferCapacity = buffer.capacity()

        // OJO! Ver que el bitmapSize no supere el bufferCapacity, sino rompe.
        Log.i("", "Bitmap size = " + bitmapSize)
        Log.i("", "Buffer size = " + bufferCapacity)

        buffer.rewind()

        encryptedImage!!.copyPixelsFromBuffer(buffer)

        // Seteamos la imagen encriptada
        (findViewById<ImageView>(R.id.ivGallery)).setImageBitmap(encryptedImage)

        Toast.makeText(applicationContext, "Imagen encriptada!", Toast.LENGTH_SHORT).show()
    }

    // Desencriptacion de la imagen seleccionada.
    fun decrypt() {

/*
        if (shouldSetSelectedImage) {
            (findViewById(R.id.ivGallery) as ImageView).setImageBitmap(originalImage)
            return
        }
*/

        // Obtenemos un byte array a partir de la imagen encriptada
        val bmp = encryptedImage

        val stream = ByteArrayOutputStream()
        bmp!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        bmp.recycle()

        // Conversion de Bitmap a ByteArray sin compresion.
/*
        var byBuffer = ByteBuffer.allocate(bmp!!.byteCount)
        bmp.copyPixelsToBuffer(byBuffer)
        byBuffer.rewind()
        val byteArray = byBuffer.array()
*/

        // Generamos la imagen para desencriptar
        val imageLoader = ImageLoader()
        val bytesHeader = imageLoader.getBytesHeader(byteArray)
        val bytesBody = imageLoader.getBytesBody(byteArray)
        val image = Image(byteArray, bytesHeader, bytesBody)

        // Obtenemos la imagen desencriptada en forma de bytes
        val decryptedBytes = triviumImageEncrypter!!.decrypt(image)

        // Convertimos los bytes en imagen
        /*val width = encryptedImage!!.width
        val height = encryptedImage!!.height*/
        val width = 200
        val height = 200

        val decryptedImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        val buffer = ByteBuffer.wrap(decryptedBytes)
        //val buffer = ByteBuffer.allocate(decryptedImage!!.getByteCount())

        val bitmapSize = decryptedImage!!.byteCount
        val bufferCapacity = buffer.capacity()

        Log.i("", "Bitmap size = " + bitmapSize)
        Log.i("", "Buffer size = " + bufferCapacity)

        buffer.rewind()

        decryptedImage.copyPixelsFromBuffer(buffer)

        // Seteamos la imagen desencriptada
        (findViewById<ImageView>(R.id.ivGallery)).setImageBitmap(decryptedImage)

        Toast.makeText(applicationContext, "Imagen desencriptada!", Toast.LENGTH_SHORT).show()

        shouldSetSelectedImage = true
    }
}
