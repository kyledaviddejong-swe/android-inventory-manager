package com.example.kyle_dejong_inventory_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private final List<InventoryItem> inventoryItems;
    private final OnInventoryItemActionListener listener;

    public InventoryAdapter(
            List<InventoryItem> inventoryItems,
            OnInventoryItemActionListener listener
    ) {
        this.inventoryItems = inventoryItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_inventory_item, parent, false);

        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {

        InventoryItem item = inventoryItems.get(position);

        holder.itemName.setText(item.getName());
        holder.quantity.setText(String.valueOf(item.getQuantity()));
        holder.location.setText(item.getLocation());

        // Zero-stock rows remain readable without relying on color alone: quantity is still shown as 0.
        boolean outOfStock = item.getQuantity() == 0;

        int background = outOfStock
                ? R.drawable.inventory_cell_zero_background
                : R.drawable.inventory_cell_background;

        holder.itemName.setBackgroundResource(background);
        holder.quantity.setBackgroundResource(background);
        holder.location.setBackgroundResource(background);
        holder.actionCell.setBackgroundResource(background);

        if (outOfStock) {
            holder.itemName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.warning_brick));
            holder.quantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.warning_brick));
            holder.itemName.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.quantity.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.itemName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            holder.quantity.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            holder.itemName.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.quantity.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // The adapter reports UI actions upward; database work remains in the Activity/helper layer.
        holder.itemView.setOnClickListener(
                v -> listener.onEditItem(item)
        );

        holder.deleteButton.setOnClickListener(
                v -> listener.onDeleteItem(item)
        );
    }

    @Override
    public int getItemCount() {
        return inventoryItems.size();
    }

    static class InventoryViewHolder extends RecyclerView.ViewHolder {

        TextView itemName;
        TextView quantity;
        TextView location;
        Button deleteButton;
        FrameLayout actionCell;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);

            itemName = itemView.findViewById(R.id.textItemName);
            quantity = itemView.findViewById(R.id.textQuantity);
            location = itemView.findViewById(R.id.textLocation);
            deleteButton = itemView.findViewById(R.id.buttonDelete);
            actionCell = itemView.findViewById(R.id.actionCell);
        }
    }

    public interface OnInventoryItemActionListener {
        void onEditItem(InventoryItem item);
        void onDeleteItem(InventoryItem item);
    }
}