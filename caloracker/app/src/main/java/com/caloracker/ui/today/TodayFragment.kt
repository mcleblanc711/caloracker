package com.caloracker.ui.today

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.caloracker.R
import com.caloracker.databinding.FragmentTodayBinding
import com.caloracker.ui.adapters.FoodLogAdapter
import com.caloracker.util.DateUtils
import com.caloracker.util.Extensions.gone
import com.caloracker.util.Extensions.showToast
import com.caloracker.util.Extensions.visible
import java.io.File

/**
 * Fragment displaying today's calorie and macro tracking.
 */
class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TodayViewModel by viewModels()
    private val adapter = FoodLogAdapter()

    private var currentPhotoUri: Uri? = null

    // Camera permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            showToast(getString(R.string.camera_permission_required))
        }
    }

    // Camera launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            // Navigate to food confirmation with image URI
            val action = TodayFragmentDirections
                .actionTodayToConfirmation(currentPhotoUri.toString())
            findNavController().navigate(action)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeData()
        updateDateDisplay()
    }

    /**
     * Set up RecyclerView for food logs.
     */
    private fun setupRecyclerView() {
        binding.rvFoodLogs.adapter = adapter
    }

    /**
     * Set up click listeners.
     */
    private fun setupClickListeners() {
        binding.fabAddFood.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }
    }

    /**
     * Observe ViewModel data.
     */
    private fun observeData() {
        // Observe food logs
        viewModel.foodLogs.observe(viewLifecycleOwner) { logs ->
            adapter.submitList(logs)

            // Show/hide empty state
            if (logs.isEmpty()) {
                binding.tvEmptyState.visible()
                binding.rvFoodLogs.gone()
            } else {
                binding.tvEmptyState.gone()
                binding.rvFoodLogs.visible()
            }
        }

        // Observe total calories
        viewModel.totalCalories.observe(viewLifecycleOwner) { calories ->
            binding.tvCalories.text = viewModel.formatCalories(calories)
        }

        // Observe calorie progress
        viewModel.calorieProgress.observe(viewLifecycleOwner) { progress ->
            binding.progressCalories.progress = progress
        }

        // Observe total macros
        viewModel.totalMacros.observe(viewLifecycleOwner) { macros ->
            binding.tvProtein.text = viewModel.formatMacros(macros.protein)
            binding.tvCarbs.text = viewModel.formatMacros(macros.carbs)
            binding.tvFat.text = viewModel.formatMacros(macros.fat)
        }
    }

    /**
     * Update date display.
     */
    private fun updateDateDisplay() {
        val today = DateUtils.getCurrentTimestamp()
        binding.tvDate.text = DateUtils.getRelativeDateString(today)
    }

    /**
     * Check camera permission and launch camera.
     */
    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Launch camera to take a photo.
     */
    private fun launchCamera() {
        // Create a file to save the photo
        val photoFile = File(
            requireContext().cacheDir,
            "food_photo_${System.currentTimeMillis()}.jpg"
        )

        currentPhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )

        takePictureLauncher.launch(currentPhotoUri)
    }

    override fun onResume() {
        super.onResume()
        // Reload totals when returning to this fragment
        viewModel.loadTotals()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
