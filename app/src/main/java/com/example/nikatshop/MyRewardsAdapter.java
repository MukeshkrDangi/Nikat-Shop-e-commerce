package com.example.nikatshop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyRewardsAdapter extends RecyclerView.Adapter<MyRewardsAdapter.Viewholder> {


    private List<RewardModel> rewardModelList;
    private Boolean useMiniLayout =false;

    public MyRewardsAdapter(List<RewardModel> rewardModelList,Boolean useMiniLayout) {
        this.rewardModelList = rewardModelList;
        this.useMiniLayout = useMiniLayout;
    }


    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (useMiniLayout){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mini_rewards_item_layout, parent, false);
        }else {

          view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rewards_item_layout, parent, false);
        }
        return new Viewholder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        String title = rewardModelList.get(position).getTitle();
        String date = rewardModelList.get(position).getExpiryDate();
        String body = rewardModelList.get(position).getCoupenBody();
        holder.setData(title,date,body);
    }

    @Override
    public int getItemCount() {
        return rewardModelList.size();
    }

    public class Viewholder extends RecyclerView.ViewHolder {


        private TextView coupenTitle;
        private TextView coupenExpiryDate;
        private TextView coupenBody;


        public Viewholder(@NonNull View itemView) {
            super(itemView);
            coupenTitle = itemView.findViewById(R.id.coupen_title);
            coupenExpiryDate =itemView.findViewById(R.id.coupen_validity);
            coupenBody = itemView.findViewById(R.id.coupen_body);
        }
        private void setData(String title, String date , String body){
            coupenTitle.setText(title);
            coupenExpiryDate.setText(date);
            coupenBody.setText(body);

            if (useMiniLayout){
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ProductDetailsActivity.coupenTitle.setText(title);
                        ProductDetailsActivity.coupenExpiryDate.setText(date);
                        ProductDetailsActivity.coupenBody.setText(body);
                        ProductDetailsActivity.showDialogRecyclerView();
                    }
                });
            }
        }
    }
}
