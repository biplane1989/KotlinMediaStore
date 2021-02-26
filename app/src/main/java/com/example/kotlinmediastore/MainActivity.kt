package com.example.kotlinmediastore

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    val TAG = "001"
    private val READ_EXTERNAL_STORAGE_REQUEST = 0x1045
    var path: String = String()
    var pathDelete: String = String()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission()
        } else {
            init()
        }
    }

    fun init() {

    }

    @SuppressLint("ResourceType")
    fun save(view: View) {
        val drawable = ContextCompat.getDrawable(applicationContext, R.raw.flower)
        val bitmap = (drawable as BitmapDrawable).bitmap

        val uri = saveImage(bitmap, this, "orange", "Flower")
        path = uri.toString()
        Log.d(TAG, "onCreate: uri: " + path)
    }

    fun get(view: View) {
        getImage(this, path)
    }

    fun getlist(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            for (item in getListImageAndroidQ()) {
                Log.d(TAG, "url image: " + item.toString())
                getImage(this, item.toString())
            }

        } else {
            for (item in getListImage()) {
                Log.d(TAG, "url image: " + item)
                getImage(this, item)
            }

        }
    }

    fun delete(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            var list = getListImageAndroidQ()
            Log.d(TAG, "delete: " + list.get(0).toString())
            deleteAndroidQ(this, list.get(0).toString())


        } else {
            Log.d(TAG, "delete: " + pathDelete)
            deleteImage(pathDelete)
        }

    }

    fun deleteAndroidQ(context: Context, path: String) {

        // Set up the projection (we only need the ID)
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = MediaStore.Images.Media.DATA + " = ?"
        val selectionArgs = arrayOf<String>(path)

        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val contentResolver: ContentResolver = contentResolver
        val c: Cursor =
            contentResolver.query(queryUri, projection, selection, selectionArgs, null)!!
        if (c.moveToFirst()) {
            // We found the ID. Deleting the item via the content provider will also remove the file
            val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            val deleteUri: Uri =
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            contentResolver.delete(deleteUri, null, null)
        } else {
            // File not found in media store DB
        }
        c.close()
    }

    fun deleteImage(url: String) {
        val fdelete = File(url)
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                Log.d("001", "deleteImage: ")
                galleryAddPic(url)
            } else {
                Log.d("001", "file not Deleted: ")
            }
        }
    }

    private fun galleryAddPic(imagePath: String) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = File(imagePath)
        val contentUri = Uri.fromFile(f)
        mediaScanIntent.data = contentUri
        sendBroadcast(mediaScanIntent)
    }

    fun getListImage(): ArrayList<String> {
        val images: ArrayList<String> = ArrayList<String>()
        images.clear()
        val uri: Uri
        val cursor: Cursor?
        var absolutePathOfImage: String? = null
        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val orderBy = MediaStore.Images.Media.DATE_TAKEN
        cursor = applicationContext.contentResolver
            .query(uri, projection, null, null, "$orderBy DESC")
        while (cursor!!.moveToNext()) {
            absolutePathOfImage =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
            Log.e("Column", absolutePathOfImage)
            images.add(absolutePathOfImage)
            pathDelete = absolutePathOfImage
//            val f = File(absolutePathOfImage)
//            val imageName: String = f.getName()
//            Log.d("001", "fn_imagespath: " + imageName)
        }

        return images
    }

    fun getListImageAndroidQ(): ArrayList<Uri> {

//        GlobalScope.launch {
        val listUris = ArrayList<Uri>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateTakenColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateTaken = Date(cursor.getLong(dateTakenColumn))
                val displayName = cursor.getString(displayNameColumn)
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                listUris.add(contentUri)

                Log.d(
                    TAG, "id: $id, display_name: $displayName, date_taken: " +
                            "$dateTaken, content_uri: $contentUri"
                )
            }
        }
        return listUris
//        }
    }


    fun getImage(context: Context, uri: String) {
        Log.d(TAG, "getImage: " + uri)
        Glide.with(context).load(uri)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(iv_flower)
    }


    // Save Image
    /// @param folderName can be your app's name
    private fun saveImage(bitmap: Bitmap, context: Context, folderName: String, name: String): Uri {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = contentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name)
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + folderName)
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            // RELATIVE_PATH and IS_PENDING are introduced in API 29.

            val uri: Uri? =
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            Log.d("002", "saveImage: ssssssssss"+uri)
            if (uri != null) {
                saveImageToStream(bitmap, context.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Video.Media.IS_PENDING, false)
                context.contentResolver.update(uri, values, null, null)
            }
            Log.d("002", "saveImage: ssssssssss"+ Uri.parse(uri.toString()))
            return Uri.parse(uri.toString())
        } else {
            // Save image to gallery
            val savedImageURL = MediaStore.Images.Media.insertImage(
                contentResolver,
                bitmap,
                name,
                "Image of $name"
            )

            // Parse the gallery image url to uri
            return Uri.parse(savedImageURL)
        }
    }

    private fun contentValues(): ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        return values
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun goToSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                val permissions = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    READ_EXTERNAL_STORAGE_REQUEST
                )
            } else {
                init()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init()
                } else {

                    val showRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                    if (!showRationale) {
                        goToSettings()
                    } else {
                        finishAffinity()
                    }
                }
                return
            }
        }
    }


}