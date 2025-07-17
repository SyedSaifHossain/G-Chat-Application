package com.syedsaifhossain.g_chatapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.syedsaifhossain.g_chatapplication.databinding.FragmentProfileBinding
import com.yalantis.ucrop.UCrop
import java.io.File

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Fragment binding is null")
    private val auth = FirebaseAuth.getInstance()
    private lateinit var database: DatabaseReference
    private val storage = FirebaseStorage.getInstance()
    private var isFragmentDestroying = false

    private val IMAGE_PICK_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance().reference

        fetchUserProfile()

        binding.profileBackImg.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_mePageFragment)
        }

        // Open gallery to pick and crop image
        binding.profilePhotoArrow.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        binding.nameArrow.setOnClickListener { showNameEditDialog() }
        binding.phoneArrow.setOnClickListener { showPhoneEditDialog() }
        binding.genderArrow.setOnClickListener { showGenderEditDialog() }
        binding.qrcodeArrow.setOnClickListener { showQRCodeEditDialog() }

        binding.regionArrow.setOnClickListener {
            findNavController().navigate(R.id.selectRegionFragment)
        }

        parentFragmentManager.setFragmentResultListener(
            "regionSelection",
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedCountry = bundle.getString("selectedCountry", "")
            _binding?.let { safeBinding ->
                safeBinding.regionNameTxt.text = selectedCountry
            }
            saveRegionToFirebase(selectedCountry)
        }
    }

    private fun fetchUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        database.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null || isFragmentDestroying || !isAdded || context == null) {
                        return
                    }
                    
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown"
                    val phone =
                        snapshot.child("phone").getValue(String::class.java) ?: "No phone number"
                    val gender = snapshot.child("gender").getValue(String::class.java) ?: "Not Set"
                    val qrCodeUrl = snapshot.child("qrCodeUrl").getValue(String::class.java) ?: ""
                    val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                        ?: snapshot.child("avatarUrl").getValue(String::class.java)

                    try {
                        _binding?.let { safeBinding ->
                            safeBinding.userNameTxt.text = name
                            safeBinding.phoneNameTxt.text = phone
                            safeBinding.genderNameTxt.text = gender

                            if (qrCodeUrl.isNotEmpty()) {
                                Glide.with(requireContext())
                                    .load(qrCodeUrl)
                                    .into(safeBinding.myqrCodeImg)
                            }

                            Glide.with(requireContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.default_avatar)
                                .override(70, 70)
                                .into(safeBinding.profilePhotoImg)
                        }
                    } catch (e: Exception) {
                        // 忽略UI更新错误
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        requireContext(),
                        "Error fetching data: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    val sourceUri = data?.data ?: return
                    val destUri = Uri.fromFile(
                        File(
                            requireContext().cacheDir,
                            "cropped_${System.currentTimeMillis()}.jpg"
                        )
                    )

                    val options = UCrop.Options().apply {
                        setToolbarColor(resources.getColor(android.R.color.black, null))
                        setStatusBarColor(resources.getColor(android.R.color.black, null))
                        setActiveControlsWidgetColor(resources.getColor(
                                android.R.color.white,
                                null
                            )
                        )
                        setToolbarWidgetColor(resources.getColor(android.R.color.white, null))
                        setFreeStyleCropEnabled(true)
                        setHideBottomControls(true)
                        setShowCropFrame(true)
                        setShowCropGrid(false)
                        setCircleDimmedLayer(false)
                    }

                    UCrop.of(sourceUri, destUri)
                        .withAspectRatio(1f, 1f)
                        .withMaxResultSize(1080, 1080)
                        .withOptions(options)
                        .start(requireContext(), this)
                }

                UCrop.REQUEST_CROP -> {
                    val resultUri = UCrop.getOutput(data!!)
                    resultUri?.let {
                        uploadProfileImage(it)
                        _binding?.let { safeBinding ->
                            safeBinding.profilePhotoImg.setImageURI(it)
                        }
                    }
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Toast.makeText(
                requireContext(),
                "Crop error: ${cropError?.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val fileRef: StorageReference = storage.reference.child("profile_images/${userId}.jpg")

        fileRef.putFile(imageUri).addOnSuccessListener {
            fileRef.downloadUrl.addOnSuccessListener { uri ->
                updateProfileImageUrl(uri.toString())
                // 检查Fragment是否还存在
                if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                    try {
                        _binding?.let { safeBinding ->
                            Glide.with(requireContext())
                                .load(uri)
                                .into(safeBinding.profilePhotoImg)
                        }
                    } catch (e: Exception) {
                        // 忽略Glide错误
                    }
                }
            }
        }.addOnFailureListener {
            if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                try {
                    Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    // 忽略Toast错误
                }
            }
        }
    }

    private fun updateProfileImageUrl(imageUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf("profileImageUrl" to imageUrl)

        database.child("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                    try {
                        Toast.makeText(
                            requireContext(),
                            "Profile image updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        // 忽略Toast错误
                    }
                }
            }
            .addOnFailureListener {
                if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                    try {
                        Toast.makeText(
                            requireContext(),
                            "Failed to update profile image",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        // 忽略Toast错误
                    }
                }
            }
    }

    private fun showNameEditDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        _binding?.let { safeBinding ->
            input.setText(safeBinding.userNameTxt.text.toString())
        }

        builder.setTitle("Edit Name")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateNameInDatabase(newName)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    private fun updateNameInDatabase(newName: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf("name" to newName)
        database.child("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                    try {
                        _binding?.let { safeBinding ->
                            safeBinding.userNameTxt.text = newName
                            Toast.makeText(requireContext(), "Name updated successfully", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } catch (e: Exception) {
                        // 忽略UI更新错误
                    }
                }
            }
            .addOnFailureListener {
                if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                    try {
                        Toast.makeText(requireContext(), "Failed to update name", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        // 忽略Toast错误
                    }
                }
            }
    }

    private fun showPhoneEditDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        _binding?.let { safeBinding ->
            input.setText(safeBinding.phoneNameTxt.text.toString())
        }

        builder.setTitle("Edit Phone Number")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val newPhone = input.text.toString().trim()
                if (newPhone.isNotEmpty()) {
                    updatePhoneInDatabase(newPhone)
                    dialog.dismiss()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Phone number cannot be empty",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    private fun updatePhoneInDatabase(newPhone: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf("phone" to newPhone)
        database.child("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                    try {
                        _binding?.let { safeBinding ->
                            safeBinding.phoneNameTxt.text = newPhone
                            Toast.makeText(
                                requireContext(),
                                "Phone number updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        // 忽略UI更新错误
                    }
                }
            }
            .addOnFailureListener {
                if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                    try {
                        Toast.makeText(
                            requireContext(),
                            "Failed to update phone number",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        // 忽略Toast错误
                    }
                }
            }
    }

    private fun showGenderEditDialog() {
        val genderOptions = arrayOf("Male", "Female", "Other")
        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle("Edit Gender")
            .setItems(genderOptions) { dialog, which ->
                val newGender = genderOptions[which]
                updateGenderInDatabase(newGender)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    private fun updateGenderInDatabase(newGender: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf("gender" to newGender)
        database.child("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                    try {
                        _binding?.let { safeBinding ->
                            safeBinding.genderNameTxt.text = newGender
                            Toast.makeText(requireContext(), "Gender updated successfully", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } catch (e: Exception) {
                        // 忽略UI更新错误
                    }
                }
            }
            .addOnFailureListener {
                if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                    try {
                        Toast.makeText(requireContext(), "Failed to update gender", Toast.LENGTH_SHORT)
                            .show()
                    } catch (e: Exception) {
                        // 忽略Toast错误
                    }
                }
            }
    }

    private fun showQRCodeEditDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val input = EditText(requireContext())
        _binding?.let { safeBinding ->
            input.setText(safeBinding.qrcodeTxt.text.toString())
        }

        builder.setTitle("Edit QR Code")
            .setView(input)
            .setPositiveButton("Generate") { dialog, _ ->
                val newQRCodeData = input.text.toString().trim()
                if (newQRCodeData.isNotEmpty()) {
                    generateQRCode(newQRCodeData)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Input cannot be empty", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    private fun generateQRCode(data: String) {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap =
                barcodeEncoder.encodeBitmap(data, com.google.zxing.BarcodeFormat.QR_CODE, 400, 400)
            _binding?.let { safeBinding ->
                safeBinding.myqrCodeImg.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                try {
                    Toast.makeText(requireContext(), "Failed to generate QR code", Toast.LENGTH_SHORT)
                        .show()
                } catch (e2: Exception) {
                    // 忽略Toast错误
                }
            }
        }
    }

    private fun saveRegionToFirebase(region: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf("region" to region)
        database.child("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                    try {
                        Toast.makeText(requireContext(), "Region updated successfully", Toast.LENGTH_SHORT)
                            .show()
                    } catch (e: Exception) {
                        // 忽略Toast错误
                    }
                }
            }
            .addOnFailureListener {
                if (_binding != null && !isFragmentDestroying && isAdded && context != null) {
                    try {
                        Toast.makeText(requireContext(), "Failed to update region", Toast.LENGTH_SHORT)
                            .show()
                    } catch (e: Exception) {
                        // 忽略Toast错误
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isFragmentDestroying = true
        (parentFragment as? HomeFragment)?.showBottomNav()
        _binding = null
    }
}