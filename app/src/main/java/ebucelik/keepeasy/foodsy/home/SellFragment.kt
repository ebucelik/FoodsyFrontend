package ebucelik.keepeasy.foodsy.home

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import ebucelik.keepeasy.foodsy.Constants.user
import ebucelik.keepeasy.foodsy.Constants.uuid
import ebucelik.keepeasy.foodsy.MainActivity
import ebucelik.keepeasy.foodsy.R
import ebucelik.keepeasy.foodsy.databinding.FragmentSellBinding
import ebucelik.keepeasy.foodsy.entitiy.Offer
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.util.*

class SellFragment(private val offer: Offer? = null) : Fragment(R.layout.fragment_sell) {

    private val REQUEST_SELECT_IMAGE_IN_ALBUM = 1
    private lateinit var binding: FragmentSellBinding
    private var imageURI: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSellBinding.bind(view)

        setEditTextValues()

        binding.mealImage.setOnClickListener {
            selectImageFromGallery()
        }

        binding.offerMealBtn.setOnClickListener {
            if(checkMealName() && checkMealCategory() && checkMealArea()){
                createOffer()
            }
        }

        setOnFocusChangeListener()
    }

    private fun setEditTextValues(){
        if(!offer?.encodedImage.isNullOrEmpty())
            binding.mealImage.setImageURI(Uri.parse(offer?.encodedImage))

        binding.offer = offer
    }

    private fun setOnFocusChangeListener(){
        binding.mealName.setOnFocusChangeListener { view, focus ->
            if(!focus){
                setOffer()
            }
        }

        binding.mealCategory.setOnFocusChangeListener { view, focus ->
            if(!focus){
                setOffer()
            }
        }

        binding.mealArea.setOnFocusChangeListener { view, focus ->
            if(!focus){
                setOffer()
            }
        }

        binding.mealIngredients.setOnFocusChangeListener { view, focus ->
            if(!focus){
                setOffer()
            }
        }
    }

    private fun setOffer(){
        (activity as HomeActivity).homeActivityViewModel.setOffer(Offer(1, binding.mealName.text.toString(), binding.mealCategory.text.toString(), binding.mealArea.text.toString(), imageURI, binding.mealIngredients.text.toString(), Date(), user))
    }

    private fun checkMealName():Boolean{
        binding.apply {
            return if (binding.mealName.length() == 0){
                binding.mealNameLayout.isHelperTextEnabled = true
                binding.mealNameLayout.error = getString(R.string.mealName)
                false
            }else{
                binding.mealNameLayout.error = null
                true
            }
        }
    }

    private fun checkMealCategory():Boolean{
        binding.apply {
            return if (binding.mealCategory.length() == 0){
                binding.mealCategoryLayout.isHelperTextEnabled = true
                binding.mealCategoryLayout.error = getString(R.string.mealCategory)
                false
            }else{
                binding.mealCategoryLayout.error = null
                true
            }
        }
    }

    private fun checkMealArea():Boolean{
        binding.apply {
            return if (binding.mealArea.length() == 0){
                binding.mealAreaLayout.isHelperTextEnabled = true
                binding.mealAreaLayout.error = getString(R.string.mealArea)
                false
            }else{
                binding.mealAreaLayout.error = null
                true
            }
        }
    }

    private fun selectImageFromGallery(){
        val intent = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_SELECT_IMAGE_IN_ALBUM)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == RESULT_OK && requestCode == REQUEST_SELECT_IMAGE_IN_ALBUM){
            val imageURI = data?.data
            this.imageURI = imageURI.toString()
            binding.mealImage.setImageURI(imageURI)
            encodeImage()
        }
    }

    private fun encodeImage():String{
        val bitmap = binding.mealImage.drawable.toBitmap()
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun createOffer(){
        val url = "${MainActivity.IP}/offer"

        val jsonObject = JSONObject()

        try {
            jsonObject
                    .put("mealName", binding.mealName.text)
                    .put("category", binding.mealCategory.text)
                    .put("area", binding.mealArea.text)
                    .put("encodedImage", encodeImage())
                    .put("ingredients", binding.mealIngredients.text)
                    .put("timestamp", LocalDateTime.now())
                    .put("userUUID", uuid)
        }catch (e: JSONException){
            e.printStackTrace()
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonObject.toString().toRequestBody(mediaType)

        val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback{
            override fun onResponse(call: Call, response: Response) {
                if(response.code == 201){
                    activity?.runOnUiThread{
                        Toast.makeText(activity, "Your meal offer is successfully created.", Toast.LENGTH_SHORT).show()
                        imageURI = ""
                        binding.mealName.setText("")
                        binding.mealCategory.setText("")
                        binding.mealArea.setText("")
                        binding.mealIngredients.setText("")
                        binding.mealImage.setImageResource(R.drawable.ic_round_add_photo_alternate_24)
                    }
                }

                response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
    }
}