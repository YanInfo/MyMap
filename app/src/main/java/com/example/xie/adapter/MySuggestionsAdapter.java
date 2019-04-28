package com.example.xie.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.xie.R;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

/**
 * 搜索提示适配器
 */
public class MySuggestionsAdapter extends SuggestionsAdapter<String, MySuggestionsAdapter.SuggestionHolder>{

    private MySuggestionsAdapter.OnItemViewClickListener listener;

    public MySuggestionsAdapter(LayoutInflater inflater) {
        super(inflater);
    }

    public void setListener(MySuggestionsAdapter.OnItemViewClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getSingleViewHeight() {
        return 50;
    }

    @Override
    public MySuggestionsAdapter.SuggestionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 布局
        View view = getLayoutInflater().inflate(R.layout.item_request, parent, false);
        return new MySuggestionsAdapter.SuggestionHolder(view);
    }

    @Override
    public void onBindSuggestionHolder(String suggestion, MySuggestionsAdapter.SuggestionHolder holder, int position) {
        holder.text.setText(getSuggestions().get(position));
    }

    /**
     * 回调接口
     */
    public interface OnItemViewClickListener {
        void OnItemClickListener(int position, View v);
    }

    class SuggestionHolder extends RecyclerView.ViewHolder {
        private final TextView text;

        public SuggestionHolder(final View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
            // 点击回调
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setTag(getSuggestions().get(getAdapterPosition()));
                    listener.OnItemClickListener(getAdapterPosition(), v);
                }
            });
        }
    }
}
