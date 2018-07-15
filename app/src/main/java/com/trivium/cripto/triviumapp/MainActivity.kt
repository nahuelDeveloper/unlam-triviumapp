package com.trivium.cripto.triviumapp

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.BitmapFactory.Options
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import java.io.*
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


/* Información útil:

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
    var editedImage: Bitmap? = null
    var encryptedImage: Bitmap? = null
    //var decryptedImage: Bitmap? = null

    var encryptedBytes: ByteArray? = null
    var decryptedBytes: ByteArray? = null
    var imageBytes: ByteArray? = null

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
            encriptar()
        }

        findViewById<Button>(R.id.btnEditImage).setOnClickListener {
            modificar()
        }

        findViewById<Button>(R.id.btnDecryptImage).setOnClickListener {
            desencriptar()
        }

        findViewById<Button>(R.id.btnInfo).setOnClickListener {
            mostrarIntegrantes()
        }

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

            /*ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, baos); //bm is the bitmap object
            byte[] b = baos.toByteArray();
            //String encodedImage = Base64.encode(b, Base64.DEFAULT);
            encodedImage = Base64.encodeBytes(b);*/

            val baos = ByteArrayOutputStream()
            img!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val b = baos.toByteArray()
            imageBytes = b
            //val encodedImage = Base64.getEncoder().encode(b)

            if (img != null) {
                //(findViewById<ImageView>(R.id.ivGallery)).setImageBitmap(pictureTurn(img, exifData))
                selectedImage = img
                (findViewById<ImageView>(R.id.ivGallery)).setImageBitmap(img)
            }
        }
    }

    // Permisos para acceder a la galeria de imagenes.
    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission for gallery denied")
            makeRequest()
        } else {
            Log.i(TAG, "Permission ok")
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
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
        originalImage = image.copy(Bitmap.Config.ARGB_8888, false)

        return image
    }

    fun initializeTriviumEncrypter(): Boolean  {
        // Generamos los parametros Random iv y key
/*
        val iv = ByteArray(80)
        Random().nextBytes(iv)
        val key = ByteArray(10)
        Random().nextBytes(key)
*/

        var inputKey = (findViewById<EditText>(R.id.editText)).text.toString()

        if (inputKey.length != 10) {
            Toast.makeText(applicationContext, "La llave debe poseer 10 digitos", Toast.LENGTH_SHORT).show()
            return false
        }

        val key = inputKey.toByteArray(Charset.defaultCharset())
        val iv = "1161688572".toByteArray(Charset.defaultCharset())

        // Inicializamos el encriptador de Trivium
        triviumImageEncrypter = TriviumImageEncrypter(key, iv)
        triviumImageEncrypter!!.initialize()

        return true
    }

    // Encriptacion de la imagen seleccionada.
    fun encriptar() {

        // Validamos que la llave sea correcta
        val result = initializeTriviumEncrypter()
        if (!result) {
            return
        }

        // Obtenemos un byte array a partir de la imagen seleccionada
        val bmp = selectedImage

        // Guardamos el bitmap como byte array en un archivo, para luego obtener los bytes con el ImageLoader
       /* val bytes = bmp!!.byteCount
        val buff = ByteBuffer.allocate(bytes)
        bmp!!.copyPixelsToBuffer(buff)
        val byteArray = buff.array()*/

        // TEST: No agarro los bytes del Bitmap sino que los tengo guardados de cuando seleccione la imagen de la galeria.
        val byteArray = imageBytes

        /*
        val path = Environment.getExternalStorageDirectory().toString()
        val file = File(path, "encrypted_image_bytes.bmp")
        val filePath = file.path

        try {
            val stream = FileOutputStream(file)
            stream.write(byteArray)
            stream.close()

        } catch (e: IOException) {
            Log.d("","ERROR: ALGO FALLO AL GRABAR LA IMAGEN ENCRIPTADA.")
        }
        */

        // Generamos la imagen para encriptar
        val imageLoader = ImageLoader()
        val bytesHeader = imageLoader.getBytesHeader(byteArray)
        val bytesBody = imageLoader.getBytesBody(byteArray)
        val image = Image(byteArray, bytesHeader, bytesBody)

        // Obtenemos la imagen encriptada en forma de bytes
        encryptedBytes = triviumImageEncrypter!!.encrypt(image)

        // Convertimos los bytes en imagen
        val buffer = ByteBuffer.wrap(encryptedBytes)

        val bitmapSize = selectedImage!!.byteCount
        val bufferCapacity = buffer.capacity()

        encryptedImage = Bitmap.createBitmap(
                bmp!!.width / 4,
                bmp!!.height / 4,
                Bitmap.Config.ARGB_8888
        )

        val bitmapSize2 = encryptedImage!!.byteCount
        val bufferCapacity2 = buffer.capacity()

        try {

            encryptedImage!!.copyPixelsFromBuffer(buffer)
            //selectedImage!!.copyPixelsFromBuffer(buffer)

            // Seteamos la imagen encriptada
            //(findViewById<ImageView>(R.id.ivGallery)).setImageBitmap(selectedImage)

            // TEST
            (findViewById<ImageView>(R.id.ivGallery)).setImageBitmap(encryptedImage)

            // Bonus: Guardamos la imagen encriptada en la galeria
//            MediaStore.Images.Media.insertImage(
//                    contentResolver,
//                    encryptedImage,
//                    "imagen_encriptada",
//                    "Imagen encriptada con Trivium"
//            )

            Toast.makeText(applicationContext, "Imagen encriptada!", Toast.LENGTH_LONG).show()
        } catch (e: OutOfMemoryError) {
            Toast.makeText(applicationContext, "Error!" + e.message, Toast.LENGTH_LONG).show()
        }
    }

    // Modificar la imagen encriptada.
    fun modificar() {

        val editImage = encryptedImage!!.copy(Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(editImage)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(61,61,255)
        paint.textSize = 100.toFloat()
        paint.setShadowLayer(1f,0f,1f,Color.BLACK)

        val bounds = Rect()
        val text = "CRIPTO"
        paint.getTextBounds(text, 0, text.length, bounds)
        val x = (encryptedImage!!.width - bounds.width()) / 2
        val y = (encryptedImage!!.height - bounds.height()) / 2
        canvas.drawText(text, x.toFloat(), y.toFloat(), paint)

        editedImage = editImage

        (findViewById<ImageView>(R.id.ivGallery)).setImageBitmap(editImage)
    }

    // Desencriptacion de la imagen seleccionada.
    fun desencriptar() {

        val bmp = editedImage

        val bytes = bmp!!.byteCount
        val buff = ByteBuffer.allocate(bytes)
        bmp!!.copyPixelsToBuffer(buff)
        val byteArray = buff.array()

        // Generamos la imagen para desencriptar
        val imageLoader = ImageLoader()
        val bytesHeader = imageLoader.getBytesHeader(byteArray)
        val bytesBody = imageLoader.getBytesBody(byteArray)
        val image = Image(byteArray, bytesHeader, bytesBody)

        // Obtenemos la imagen desencriptada en forma de bytes
        val decryptedBytes = triviumImageEncrypter!!.decrypt(image)

        // Convertimos los bytes en imagen
        val buffer = ByteBuffer.wrap(decryptedBytes)

        val bitmapSize = selectedImage!!.byteCount
        val bufferCapacity = buffer.capacity()

        try {
            selectedImage!!.copyPixelsFromBuffer(buffer)

            // Seteamos la imagen encriptada
            (findViewById<ImageView>(R.id.ivGallery)).setImageBitmap(selectedImage)

            Toast.makeText(applicationContext, "Imagen desencriptada!", Toast.LENGTH_LONG).show()
        } catch (e: OutOfMemoryError) {
            Toast.makeText(applicationContext, "Error!" + e.message, Toast.LENGTH_LONG).show()
        }
    }

    fun mostrarIntegrantes() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Roldan - Reskin - Ciccone - Ron")
        builder.show()
    }

    fun encrypt() {
        Log.i(TAG, "Encriptar imagen")

        val result = initializeTriviumEncrypter()

        if (!result) {
            return
        }

        // Obtenemos un byte array a partir de la imagen seleccionada
        val bmp = selectedImage

        // Conversion de Bitmap a ByteArray con compresion.
//        val stream = ByteArrayOutputStream()
//        bmp!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
//        val byteArray = stream.toByteArray()
//        bmp.recycle()

        // Conversion de Bitmap a ByteArray sin compresion.
//        var byBuffer = ByteBuffer.allocate(bmp!!.byteCount)
//        bmp.copyPixelsToBuffer(byBuffer)
//        byBuffer.rewind()
//        val byteArray = byBuffer.array()

        // Guardamos el bitmap como byte array en un archivo, para luego obtener los bytes con el ImageLoader
        /*val bytes = bmp!!.byteCount
        val buff = ByteBuffer.allocate(bytes)
        bmp!!.copyPixelsToBuffer(buff)
        val array = buff.array()
        val path = "img.bmp"
        val fos = openFileOutput(path, Context.MODE_PRIVATE)
        fos.write(array)*/

        val bytes = bmp!!.byteCount
        val buff = ByteBuffer.allocate(bytes)
        bmp!!.copyPixelsToBuffer(buff)
        val byteArray = buff.array()
        val path = Environment.getExternalStorageDirectory().toString()
        val file = File(path, "encrypted_image_bytes.bmp")
        val filePath = file.path

        try {
            val stream = FileOutputStream(file)
            stream.write(byteArray)
            stream.close()

        } catch (e: IOException) {
            Log.d("","ERROR: ALGO FALLO AL GRABAR LA IMAGEN ENCRIPTADA.")
        }

        // Generamos la imagen para encriptar
        val imageLoader = ImageLoader()
        val byteArray2 = imageLoader.getBytes(filePath)
        val bytesHeader = imageLoader.getBytesHeader(byteArray)
        val bytesBody = imageLoader.getBytesBody(byteArray)
        val image = Image(byteArray, bytesHeader, bytesBody)

        // Obtenemos la imagen encriptada en forma de bytes
        encryptedBytes = triviumImageEncrypter!!.encrypt(image)

        // OPCION 2: Guardo el byte array en un archivo temporal, y luego lo levanto como Bitmap
        /*
        val path = Environment.getExternalStorageDirectory().toString()
        val file = File(path, "encrypted_image.jpg")

        val filePath = file.path
        val fileAbsolutePath = file.absolutePath

        try {
            val stream = FileOutputStream(file)
            stream.write(encryptedBytes)
            stream.close()

        } catch (e: IOException) {
            Log.d("","ERROR: ALGO FALLO AL GRABAR LA IMAGEN ENCRIPTADA.")
        }

        try {
            val fis = FileInputStream(fileAbsolutePath)
            //val bitmap = BitmapFactory.decodeFile(fileAbsolutePath)
            val bitmap = BitmapFactory.decodeStream(fis)
            findViewById<ImageView>(R.id.ivGallery).setImageBitmap(bitmap)

            if (bitmap == null) {
                val fis = FileInputStream(fileAbsolutePath)
                val options: BitmapFactory.Options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                var bitmap = BitmapFactory.decodeStream(fis, null, options)
                try {
                    fis.reset()
                } catch (e: IOException) {
                    Log.d("", e.toString())
                }

                val width: Int = selectedImage!!.width
                val height: Int = selectedImage!!.height
                options.inSampleSize = calculateSampleSize(options, width, height)
                options.inJustDecodeBounds = false
                bitmap = BitmapFactory.decodeStream(fis, null, options)
                findViewById<ImageView>(R.id.ivGallery).setImageBitmap(bitmap)
            }


        } catch (e: OutOfMemoryError) {
            Log.d("","ERROR: FALTA MEMORIA PARA CREAR EL BITMAP" + e)

            try {

                val options = Options()
                //options.inPreferredConfig = Bitmap.Config.ARGB_8888
                options.inSampleSize = 2
                val bitmap = BitmapFactory.decodeFile(filePath, options)
                findViewById<ImageView>(R.id.ivGallery).setImageBitmap(bitmap)

            } catch (e2: Exception) {
                Log.d("", "ERROR: FALLO EL BITMAP" + e2)
            }
        }
        */

        // OPCION 1: Convertimos los bytes en imagen
        val width = selectedImage!!.width * 1.4
        val height = selectedImage!!.height * 1.4
        encryptedImage = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        //encryptedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        //encryptedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444)
        //encryptedImage = Bitmap.createBitmap(width, height, Bitmap.Config.HARDWARE) (API 26)
        //encryptedImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGBA_F16) (API 26)

        val byteArrayLength = encryptedBytes!!.count()

        //val bitmap = BitmapFactory.decodeByteArray(encryptedBytes, 0, byteArrayLength)

        val buffer = ByteBuffer.wrap(encryptedBytes)
        //val buffer = ByteBuffer.allocate(encryptedImage!!.getByteCount())

        val bitmapSize = encryptedImage!!.byteCount
        val bufferCapacity = buffer.capacity()

        // OJO! Ver que el bitmapSize no supere el bufferCapacity, sino rompe.
        Log.i("", "Bitmap size = " + bitmapSize)
        Log.i("", "Buffer size = " + bufferCapacity)

        //buffer.rewind()

        try {
            encryptedImage!!.copyPixelsFromBuffer(buffer)

            // Seteamos la imagen encriptada
            (findViewById<ImageView>(R.id.ivGallery)).setImageBitmap(encryptedImage)

            Toast.makeText(applicationContext, "Imagen encriptada!", Toast.LENGTH_LONG).show()
        } catch (e: OutOfMemoryError) {
            Toast.makeText(applicationContext, "Error!" + e.message, Toast.LENGTH_LONG).show()
        }
    }

    // Desencriptacion de la imagen seleccionada.
    fun decrypt() {

        // Logica fantasma para volver a cargar la imagen original al momento de desencriptar... :p
/*
        if (shouldSetSelectedImage) {
            (findViewById(R.id.ivGallery) as ImageView).setImageBitmap(originalImage)
            return
        }
*/

        // Obtenemos un byte array a partir de la imagen encriptada
//        val bmp = encryptedImage
//        val stream = ByteArrayOutputStream()
//        bmp!!.compress(Bitmap.CompressFormat.JPEG, 100, stream)
//        val byteArray = stream.toByteArray()
//        bmp.recycle()

        val byteArray = encryptedBytes

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
        val width = encryptedImage!!.width
        val height = encryptedImage!!.height
//        val width = 200
//        val height = 200

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

    // NO soporta menos de API 27
/*    fun opcion2() {
        val imagePath = System.getProperty("user.dir") + "\\src\\encrypted_image.JPEG"

        try {
            Files.write(Paths.get(imagePath), encryptedBytes)

            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bitmap = BitmapFactory.decodeFile(imagePath, options)
            findViewById<ImageView>(R.id.ivGallery).setImageBitmap(bitmap)

        } catch (e: IOException) {
            Log.d("","ERROR: No se pudo grabar la imagen")
        }
    }*/

    fun calculateSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ( (halfHeight / inSampleSize) >= reqHeight  && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
