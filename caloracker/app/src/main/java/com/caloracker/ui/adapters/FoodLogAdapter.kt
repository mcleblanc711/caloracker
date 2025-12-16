package com.caloracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.caloracker.databinding.ItemFoodLogBinding
import com.caloracker.domain.model.Food

/**
 * RecyclerView adapter for displaying food log items.
 */
class FoodLogAdapter : ListAdapter<Food, FoodLogAdapter.FoodLogViewHolder>(FoodLogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodLogViewHolder {
        val binding = ItemFoodLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FoodLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for food log items.
     */
    class FoodLogViewHolder(
        private val binding: ItemFoodLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(food: Food) {
            binding.apply {
                tvFoodName.text = food.name
                tvPortion.text = food.portion
                tvCalories.text = food.nutrition.calories.toInt().toString()
                tvMacros.text = formatMacros(food.nutrition)
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
    class FoodLogDiffCallback : DiffUtil.ItemCallback<Food>() {
        override fun areItemsTheSame(oldItem: Food, newItem: Food): Boolean {
            return oldItem.name == newItem.name && oldItem.portion == newItem.portion
        }

        override fun areContentsTheSame(oldItem: Food, newItem: Food): Boolean {
            return oldItem == newItem
        }
    }
}
