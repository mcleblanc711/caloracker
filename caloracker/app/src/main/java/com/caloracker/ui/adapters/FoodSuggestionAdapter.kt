package com.caloracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.caloracker.databinding.ItemFoodSuggestionBinding
import com.caloracker.domain.model.Food

/**
 * RecyclerView adapter for displaying food suggestion items.
 */
class FoodSuggestionAdapter(
    private val onItemClick: (Food) -> Unit
) : ListAdapter<Food, FoodSuggestionAdapter.FoodSuggestionViewHolder>(FoodSuggestionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodSuggestionViewHolder {
        val binding = ItemFoodSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FoodSuggestionViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: FoodSuggestionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for food suggestion items.
     */
    class FoodSuggestionViewHolder(
        private val binding: ItemFoodSuggestionBinding,
        private val onItemClick: (Food) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(food: Food) {
            binding.apply {
                tvFoodName.text = food.name
                tvPortion.text = food.portion
                tvCalories.text = food.nutrition.calories.toInt().toString()
                tvMacros.text = formatMacros(food.nutrition)

                root.setOnClickListener {
                    onItemClick(food)
                }
            }
        }

        private fun formatMacros(nutrition: com.caloracker.domain.model.NutritionInfo): String {
            return "P:${nutrition.protein.toInt()}g | " +
                    "C:${nutrition.carbs.toInt()}g | " +
                    "F:${nutrition.fat.toInt()}g"
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    class FoodSuggestionDiffCallback : DiffUtil.ItemCallback<Food>() {
        override fun areItemsTheSame(oldItem: Food, newItem: Food): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Food, newItem: Food): Boolean {
            return oldItem == newItem
        }
    }
}
