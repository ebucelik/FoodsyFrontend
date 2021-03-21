package ebucelik.keepeasy.foodsy.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.tabs.TabLayout
import com.google.gson.GsonBuilder
import ebucelik.keepeasy.foodsy.MainActivity
import ebucelik.keepeasy.foodsy.R
import ebucelik.keepeasy.foodsy.account.OfferFragment
import ebucelik.keepeasy.foodsy.account.OrderFragment
import ebucelik.keepeasy.foodsy.entitiy.Order
import ebucelik.keepeasy.foodsy.entitiy.User
import okhttp3.*
import java.io.IOException
import java.util.ArrayList
import kotlin.math.round

class AccountFragment(home: HomeActivity, private val uuid: String) : Fragment(R.layout.fragment_account) {

    private lateinit var offerFragment: OfferFragment
    private lateinit var orderFragment: OrderFragment
    private val homeActivity: HomeActivity = home
    private lateinit var rates: ArrayList<ImageView>
    private lateinit var user: User
    private lateinit var firstnameView: TextView
    private lateinit var lastnameView: TextView
    private lateinit var usernameView: TextView
    private lateinit var profileImage: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Binding don't work. Don't know why exactly but with the included TabItems it don't work.
        val accountTabLayout = view.findViewById<TabLayout>(R.id.accountTabLayout)
        firstnameView = view.findViewById(R.id.firstname)
        lastnameView = view.findViewById(R.id.lastname)
        usernameView = view.findViewById(R.id.username)
        profileImage = view.findViewById(R.id.profileImage)

        getUser()

        rates = arrayListOf(
                view.findViewById(R.id.rate1),
                view.findViewById(R.id.rate2),
                view.findViewById(R.id.rate3),
                view.findViewById(R.id.rate4),
                view.findViewById(R.id.rate5)
        )

        offerFragment = OfferFragment(uuid)
        orderFragment = OrderFragment(uuid, homeActivity)

        getStarReview()

        accountTabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when(tab?.position){
                    0 -> changeFragment(offerFragment)
                    1 -> changeFragment(orderFragment)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        changeFragment(offerFragment)
    }

    private fun changeFragment(fragment: Fragment){
        homeActivity.supportFragmentManager.beginTransaction().apply {
            replace(R.id.fLAccountFragment, fragment)
            commit()
        }
    }

    private fun getStarReview(){
        val url = "http://${MainActivity.IP}:8080/reviewAverage?uuid=$uuid"

        val request = Request.Builder()
                .url(url)
                .get()
                .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object: Callback{
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                if(response.code == 200){
                    if (body != null) {
                        setStarReviews(body.toInt())
                    }
                }

                response.close()
            }

            override fun onFailure(call: Call, e: IOException) {}
        })
    }

    private fun setStarReviews(reviewInt:Int){
        if(reviewInt >= 1){
            for (i in 0 until reviewInt){
                rates[i].setImageResource(R.drawable.ic_round_star_rate_24_gold)
            }
        }
    }

    private fun getUser(){
        val url = "http://${MainActivity.IP}:8080/user?userUUID=$uuid"

        val request = Request.Builder()
                .url(url)
                .get()
                .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response){
                val body = response.body?.string()

                val gson = GsonBuilder().create()
                user = gson.fromJson(body, User::class.java)

                activity?.runOnUiThread {
                    when(response.code){
                        200 -> {
                            firstnameView.text = user.firstname
                            lastnameView.text = user.surname
                            usernameView.text = user.username
                            profileImage.setImageBitmap(decodeImage(user.profileImage))
                        }
                        404 -> {
                            Toast.makeText(activity, "User not found.", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(activity, "Server error.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
    }

    private fun decodeImage(encodedImage: String): Bitmap {
        val imageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}