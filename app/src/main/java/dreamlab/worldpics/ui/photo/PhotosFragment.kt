package dreamlab.worldpics.ui.photo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.DaggerFragment
import dreamlab.worldpics.databinding.FragmentPhotosBinding
import dreamlab.worldpics.model.Photo
import dreamlab.worldpics.util.viewModelProvider
import timber.log.Timber
import javax.inject.Inject

class PhotosFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PhotoViewModel

    private lateinit var mBinding: FragmentPhotosBinding
    private lateinit var mAdapter: PhotoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        viewModel = viewModelProvider(viewModelFactory)

        mAdapter = PhotoAdapter(::onPhotoClicked)
        mBinding = FragmentPhotosBinding.inflate(inflater, container, false)

        initAdapter()

        viewModel.searchPhotos(null)

        return mBinding.root
    }

    fun onPhotoClicked(photo: Photo) {
        //TODO
    }

    fun removeBannerAds() {
        //TODO
    }

    private fun initAdapter() {
        mBinding.recycler.adapter = mAdapter
        viewModel.photos.observe(viewLifecycleOwner, Observer {
            Timber.d("list: ${it?.size}")
            mAdapter.submitList(it)
        })
        viewModel.networkErrors.observe(viewLifecycleOwner, Observer {
            Toast.makeText(requireContext(), "\uD83D\uDE28 Wooops $it", Toast.LENGTH_LONG).show()
        })
    }

}
