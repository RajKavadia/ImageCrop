package com.example.myapplication.ui.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.dhaval2404.imagepicker.ImagePicker

class   MainViewModel : ViewModel() {

    private var CAMERA_PERMISSION_REQUEST_CODE:Int=1223;
    val imageUri: MutableLiveData<String> = MutableLiveData()
    init {
        imageUri.value=""
    }
    val showPicker: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessageId: MutableLiveData<Int> = MutableLiveData()
    val checkGalleryPermission: MutableLiveData<Boolean> = MutableLiveData()
    fun gotGalleryPermissionStatus(isGranted: Boolean) {
        if (isGranted) {
            showPicker.value = true
        } else {
            checkGalleryPermission.value = true
        }
    }
    fun onRequestPermissionsResult(requestCode: Int, isGranted: Boolean) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (isGranted) {
                    showPicker.value = true
                } else {
                    errorMessageId.value = 1;
                }
            }
        }
    }




}