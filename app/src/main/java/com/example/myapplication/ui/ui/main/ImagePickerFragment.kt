package com.example.myapplication.ui.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ImageCropper.R
import com.airbnb.lottie.LottieAnimationView
import com.isseiaoki.simplecropview.CropImageView
import com.isseiaoki.simplecropview.callback.CropCallback
import java.io.*
import kotlin.collections.ArrayList


class ImagePickerFragment : Fragment() {

    companion object {
        fun newInstance() = ImagePickerFragment()
    }

    init {
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var pickImage: LottieAnimationView;
    private lateinit var save: LottieAnimationView;
    private lateinit var cropview: CropImageView
    private val PICK_FROM_GALLARY = 2
    private lateinit var rotate_vertical:ImageView
    private lateinit var crop:ImageView

    private lateinit var rotate_horizontal:ImageView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        checkAndRequestPermissions()
        var view = inflater.inflate(R.layout.main_fragment, container, false)
        cropview = view.findViewById(R.id.cropview)
        pickImage = view.findViewById(R.id.lottie_pick);
        save = view.findViewById(R.id.lottie_save);
        rotate_horizontal=view.findViewById(R.id.rotate_horizontal)
        rotate_vertical=view.findViewById(R.id.rotate_vertical)
        rotate_vertical.rotation=90.0F
        crop= view.findViewById(R.id.crop)
        return view;
    }

    @SuppressLint("IntentReset")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        checkAndRequestPermissions()
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.checkGalleryPermission.observe(requireActivity(), Observer {
            when {
                it -> {
                    makeText(requireContext(), "Permissionn Given", Toast.LENGTH_SHORT);

                }
                else -> {
                    makeText(requireContext(), "Please enable the permission", Toast.LENGTH_SHORT);
                }
            }
        })
        pickImage.setOnClickListener {
            if (checkAndRequestPermissions()) {
                val galleryIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                // Start the Intent
                galleryIntent.setType("image/*");
                // Start the Intent
                startActivityForResult(galleryIntent, PICK_FROM_GALLARY)
            }
        }
        rotate_vertical.setOnClickListener{
            if(cropview.imageBitmap!=null){
                cropview.imageBitmap= flip(cropview.imageBitmap,1);
            }

        }
        rotate_horizontal.setOnClickListener{
            if(cropview.imageBitmap!=null) {
                cropview.imageBitmap= flip(cropview.imageBitmap,2);

            }

        }
        crop.setOnClickListener{
            if(cropview.imageBitmap!=null){
                cropview.crop(Uri.parse(uri))
                    .execute(object : CropCallback {
                        override fun onSuccess(cropped: Bitmap) {
                            cropview.imageBitmap=cropped;
                        }
                        override fun onError(e: Throwable) {}
                    })

            }
        }
        save.setOnClickListener {
            if (cropview.imageBitmap!=null)
            saveImage(cropview.imageBitmap)
            Toast.makeText(requireContext(),"Image Saved",Toast.LENGTH_SHORT)
        }
    }


    private fun checkAndRequestPermissions(): Boolean {
        val REQUEST_ID_MULTIPLE_PERMISSIONS = 1
        Log.d("checking permission", "checkAndRequestPermissions: ")
        val storage =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        val write =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                listPermissionsNeeded.toTypedArray(),
                REQUEST_ID_MULTIPLE_PERMISSIONS
            )
            return false
        }
        return true
    }
    lateinit var uri: String;
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
            if (resultCode == Activity.RESULT_OK) {
                Log.d("result ok", "onActivityResult: ")
                val selectedImageUri: String? = data!!.toURI()
                uri= selectedImageUri!!
                cropview.setImageURI(Uri.parse(selectedImageUri))
            }
    }
    fun createDirIfNotExists(path: String?): Boolean {
        var ret = true
        val file = File(Environment.getExternalStorageDirectory(), path)
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e("TravellerLog :: ", "Problem creating Image folder")
                ret = false
            }
        }
        return ret
    }
    val FLIP_VERTICAL = 1
    val FLIP_HORIZONTAL = 2
    fun flip(src: Bitmap, type: Int): Bitmap? {
        // create new matrix for transformation
        val matrix = Matrix()
        // if vertical
        if (type == FLIP_VERTICAL) {
            matrix.preScale(1.0f, -1.0f)
        } else if (type == FLIP_HORIZONTAL) {
            matrix.preScale(-1.0f, 1.0f)
            // unknown type
        } else {
            return null
        }

        // return transformed image
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }
    private fun saveImage(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = contentValues()
            values.put(Images.Media.RELATIVE_PATH, "Pictures/" + getString(R.string.app_name))
            values.put(Images.Media.IS_PENDING, true)
            val uri: Uri? =
                requireActivity().getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                try {
                    saveImageToStream(bitmap, requireActivity().getContentResolver().openOutputStream(uri))
                    values.put(Images.Media.IS_PENDING, false)
                    requireActivity().getContentResolver().update(uri, values, null, null)
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        } else {
            val directory = File(
                Environment.getExternalStorageDirectory()
                    .toString() + '/' + getString(R.string.app_name)
            )
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = System.currentTimeMillis().toString() + ".png"
            val file = File(directory, fileName)
            try {
                saveImageToStream(bitmap, FileOutputStream(file))
                val values = ContentValues()
                values.put(Images.Media.DATA, file.absolutePath)
                requireActivity().getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    private fun contentValues(): ContentValues {
        val values = ContentValues()
        values.put(Images.Media.MIME_TYPE, "image/png")
        values.put(Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }
        return values
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }
    @Throws(IOException::class)
    fun saveImageToExternal(imgName: String, bm: Bitmap) {
        //Create Path to save Image
        val path =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) //Creates app specific folder
        val imageFile = File(path, "$imgName.png") // Imagename.png
        val out = FileOutputStream(imageFile)
        try {
            bm.compress(Bitmap.CompressFormat.PNG, 100, out) // Compress Image
            out.flush()
            out.close()

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(
                context, arrayOf(imageFile.absolutePath), null
            ) { path, uri ->
                Log.i("ExternalStorage", "Scanned $path:")
                Log.i("ExternalStorage", "-> uri=$uri")
            }
        } catch (e: Exception) {
            throw IOException()
        }
    }

}