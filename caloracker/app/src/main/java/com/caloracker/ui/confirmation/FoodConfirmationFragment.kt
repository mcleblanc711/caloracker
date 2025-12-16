package com.caloracker.ui.confirmation

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.caloracker.R
import com.caloracker.databinding.FragmentFoodConfirmationBinding
import com.caloracker.domain.model.Food
import com.caloracker.ui.adapters.FoodSuggestionAdapter
import com.caloracker.util.gone
import com.caloracker.util.showToast
import com.caloracker.util.visible

/**
 * Fragment for confirming food detection from Claude vision API.
 * Shows primary food identification and alternative suggestions.
 */
class FoodConfirmationFragment : Fragment() {

    private var _binding: FragmentFoodConfirmationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FoodConfirmationViewModel by viewModels()
    private val args: FoodConfirmationFragmentArgs by navArgs()

    private val alternativesAdapter = FoodSuggestionAdapter { food ->
        // User selected an alternative food
        viewModel.selectFood(food)
        updateSelectedFoodDisplay(food)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoodConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeData()
        loadImage()
        analyzeImage()
    }

    /**
     * Set up RecyclerView for alternative suggestions.
     */
    private fun setupRecyclerView() {
        binding.rvAlternatives.adapter = alternativesAdapter
    }

    /**
     * Set up click listeners.
     */
    private fun setupClickListeners() {
        binding.btnConfirm.setOnClickListener {
            viewModel.logSelectedFood()
        }

        binding.btnRetry.setOnClickListener {
            // Navigate back to take a new photo
            findNavController().navigateUp()
        }
    }

    /**
     * Observe ViewModel data.
     */
    private fun observeData() {
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.progressLoading.visible()
                binding.layoutButtons.gone()
            } else {
                binding.progressLoading.gone()
            }
        }

        // Observe error
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.tvError.text = error
                binding.tvError.visible()
                binding.cardPrimaryFood.gone()
                binding.tvAlternativesHeader.gone()
                binding.rvAlternatives.gone()
                binding.layoutButtons.gone()
            } else {
                binding.tvError.gone()
            }
        }

        // Observe selected food (primary)
        viewModel.selectedFood.observe(viewLifecycleOwner) { food ->
            food?.let {
                updateSelectedFoodDisplay(it)
                binding.cardPrimaryFood.visible()
                binding.layoutButtons.visible()
            }
        }

        // Observe alternatives
        viewModel.alternatives.observe(viewLifecycleOwner) { alternatives ->
            if (alternatives.isNotEmpty()) {
                alternativesAdapter.submitList(alternatives)
                binding.tvAlternativesHeader.visible()
                binding.rvAlternatives.visible()
            } else {
                binding.tvAlternativesHeader.gone()
                binding.rvAlternatives.gone()
            }
        }

        // Observe food logged success
        viewModel.foodLogged.observe(viewLifecycleOwner) { logged ->
            if (logged) {
                showToast(getString(R.string.food_added_success))
                // Navigate back to today screen
                findNavController().navigateUp()
            }
        }
    }

    /**
     * Load and display the captured image.
     */
    private fun loadImage() {
        val imageUri = Uri.parse(args.imageUri)
        binding.ivFoodImage.load(imageUri) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_foreground)
        }
    }

    /**
     * Analyze the image using Claude vision API.
     */
    private fun analyzeImage() {
        val imageUri = Uri.parse(args.imageUri)
        viewModel.analyzeFoodImage(imageUri)
    }

    /**
     * Update the display of the selected food.
     */
    private fun updateSelectedFoodDisplay(food: Food) {
        binding.apply {
            tvPrimaryFoodName.text = food.name
            tvPrimaryPortion.text = "Portion: ${food.portion}"
            tvPrimaryCalories.text = "${food.nutrition.calories.toInt()} cal"
            tvPrimaryMacros.text = formatMacros(food)
        }
    }

    /**
     * Format macros for display.
     */
    private fun formatMacros(food: Food): String {
        return "Protein: ${food.nutrition.protein.toInt()}g | " +
                "Carbs: ${food.nutrition.carbs.toInt()}g | " +
                "Fat: ${food.nutrition.fat.toInt()}g"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
