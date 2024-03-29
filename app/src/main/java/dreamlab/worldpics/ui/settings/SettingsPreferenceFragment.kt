package dreamlab.worldpics.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.codemybrainsout.ratingdialog.RatingDialog
import com.google.firebase.database.FirebaseDatabase
import dagger.android.support.AndroidSupportInjection
import dreamlab.worldpics.AutoWallpaperWorker
import dreamlab.worldpics.BuildConfig
import dreamlab.worldpics.R
import dreamlab.worldpics.WorldPics
import dreamlab.worldpics.billing.BillingManager
import dreamlab.worldpics.model.Feedback
import dreamlab.worldpics.util.FileUtils
import dreamlab.worldpics.util.SharedPreferenceStorage
import dreamlab.worldpics.util.SharedPreferenceStorage.Companion.PREFERENCE_ABOUT_ME
import dreamlab.worldpics.util.SharedPreferenceStorage.Companion.PREFERENCE_CLEAR_CACHE
import dreamlab.worldpics.util.SharedPreferenceStorage.Companion.PREFERENCE_DONATE
import dreamlab.worldpics.util.SharedPreferenceStorage.Companion.PREFERENCE_PRIVACY
import dreamlab.worldpics.util.SharedPreferenceStorage.Companion.PREFERENCE_RATE_US
import dreamlab.worldpics.util.SharedPreferenceStorage.Companion.PREFERENCE_VERSION
import dreamlab.worldpics.util.SharedPreferenceStorage.Companion.PREFERENCE_VISIT_PIXABAY
import dreamlab.worldpics.util.SharedPreferenceStorage.Companion.PREFERENCE_WORK_MANAGER
import dreamlab.worldpics.util.viewModelProvider
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class SettingsPreferenceFragment : PreferenceFragmentCompat() {

    companion object {
        val SETTINGS_PREFERENCE_FRAGMENT_TAG = "SETTINGS_PREFERENCE_FRAGMENT"
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: SettingsViewModel

    @Inject
    lateinit var mSharedPreferenceStorage: SharedPreferenceStorage

    private lateinit var billingManager: BillingManager

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        billingManager = BillingManager(requireActivity())

        activity?.let {
            view.setBackgroundColor(
                ContextCompat.getColor(
                    it,
                    R.color.background_material_light
                )
            ) //REMIND: fix al problema tra ripple effect al click e background
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.info, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = viewModelProvider(viewModelFactory)

        val buttonRateUs: Preference? = findPreference(PREFERENCE_RATE_US)
        buttonRateUs?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.let {
                RatingDialog.Builder(activity)
                    .threshold(3f)
                    .playstoreUrl("https://play.google.com/store/apps/details?id=dreamlab.worldpics&hl=en_US")
                    .onRatingBarFormSumbit { feedback ->
                        val df = SimpleDateFormat("dd MM yyyy HH:mm:ss")
                        val today = Calendar.getInstance().time
                        val reportDate = df.format(today)

                        val database = FirebaseDatabase.getInstance()
                        val reference = database.getReference("feedback")

                        reference.child(reportDate).setValue(
                            Feedback(feedback, BuildConfig.VERSION_NAME, Build.VERSION.SDK_INT)
                        )

                        Toast.makeText(activity, "Thank you!", Toast.LENGTH_SHORT).show()
                    }.build().show()
                true
            }
            false
        }

        val version: Preference? = findPreference(PREFERENCE_VERSION)
        version?.summary = BuildConfig.VERSION_NAME

        val cache: Preference? = findPreference(PREFERENCE_CLEAR_CACHE)

        activity?.let {
            val startCacheSize = FileUtils.getCacheSize(it)
            cache?.summary = String.format("Cache size: %s", startCacheSize)
        }

        cache?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            viewModel.clearCache(activity)
            activity?.let {
                val startCacheSize = FileUtils.getCacheSizeInMB(requireContext())
                cache?.summary = String.format("Cache size: %d MB", startCacheSize)
                Toast.makeText(activity, "Cache cleared!", Toast.LENGTH_SHORT).show()
            }
            true
        }

        val privacyPolicy: Preference? = findPreference(PREFERENCE_PRIVACY)
        privacyPolicy?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WorldPics.PRIVACY_POLICY_URL))
            startActivity(intent)
            true
        }

        val visitPixabay: Preference? = findPreference(PREFERENCE_VISIT_PIXABAY)
        visitPixabay?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pixabay.com/"))
            startActivity(intent)
            true
        }

        val donate: Preference? = findPreference(PREFERENCE_DONATE)
        donate?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            billingManager.launchBillingFlow()
            true
        }

        val about: Preference? = findPreference(PREFERENCE_ABOUT_ME)
        about?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://it.linkedin.com/in/daniel-milano-6b9a42134")
            )
            startActivity(intent)
            true
        }

        setAutoWallpaper()

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    fun updateCacheSummary() {
        val startCacheSize = FileUtils.getCacheSize(requireContext())
        val cache: Preference? = findPreference(PREFERENCE_CLEAR_CACHE)
        cache?.summary = String.format("Cache size: %s", startCacheSize)
    }

    private fun setAutoWallpaper() {
        val autowallpaper: Preference? = findPreference(PREFERENCE_WORK_MANAGER)
        viewModel.getFavouritePhotos()?.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                mSharedPreferenceStorage.preferenceAutowallpaper = null
                viewModel.cancelWork()
                autowallpaper?.summary =
                    "Will set your device wallpaper with a random photo taken from your fovourites photo"
                autowallpaper?.isVisible = false
            } else {
                if (!mSharedPreferenceStorage.preferenceAutowallpaper.isNullOrEmpty()) {
                    autowallpaper?.summary =
                        "Every ${mSharedPreferenceStorage.preferenceAutowallpaper} hours your wallpaper will be automatically set with a random photo taken from your fovourites photo"
                } else {
                    autowallpaper?.summary =
                        "Will set your device wallpaper with a random photo taken from your fovourites photo"
                }
                autowallpaper?.isVisible = true
            }
        }
        autowallpaper?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { pref, newValue ->
                if (newValue?.toString()?.isEmpty() == true) {
                    autowallpaper?.summary =
                        "Will set your device wallpaper with a random photo taken from your fovourites photo"
                } else {
                    mSharedPreferenceStorage.preferenceAutowallpaper = newValue.toString()
                    autowallpaper?.summary =
                        "Every ${mSharedPreferenceStorage.preferenceAutowallpaper} hours your wallpaper will be automatically set with a random photo taken from your fovourites photo"
                    viewModel.setAutoWallpaper(newValue.toString().toLong())
                }
                true
            }
    }
}