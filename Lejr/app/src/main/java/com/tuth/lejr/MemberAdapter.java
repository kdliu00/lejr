package com.tuth.lejr;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberHolder> {

    private List<Member> mMemberList;
    private CreateEntryFragment fragment;

    public MemberAdapter(List<Member> modelList, CreateEntryFragment _fragment) {
        mMemberList = modelList;
        fragment = _fragment;
    }

    @Override
    public MemberHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_item, parent, false);
        return new MemberHolder(view);
    }

    @Override
    public void onBindViewHolder(final MemberHolder holder, int position) {
        final Member model = mMemberList.get(position);
        holder.textView.setText(model.getName());
        holder.view.setBackgroundColor(model.isSelected() ? Color.CYAN : Color.WHITE);
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                model.setSelected(!model.isSelected());
                holder.view.setBackgroundColor(model.isSelected() ? Color.CYAN : Color.WHITE);
                fragment.isSelected.put(model.getUserID(), model.isSelected());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mMemberList == null ? 0 : mMemberList.size();
    }

    public class MemberHolder extends RecyclerView.ViewHolder {

        private View view;
        private TextView textView;

        private MemberHolder(View itemView) {
            super(itemView);
            view = itemView;
            textView = itemView.findViewById(R.id.text_view_name);
        }
    }

}
