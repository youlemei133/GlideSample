package com.hudawei.glidesample;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Created by hudawei on 2018/4/27.
 * a
 */

public class WelcomeActivity extends Activity {

    private YtaRecyclerView recyclerView;
    //    private LinearLayoutManager manager;
    private Yta2LayoutManager manager;
    private RecycleAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        recyclerView = findViewById(R.id.recyclerView);

//        manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        manager = new Yta2LayoutManager();
        recyclerView.setLayoutManager(manager);
        mAdapter = new RecycleAdapter();
        recyclerView.setAdapter(mAdapter);

    }

    public void test(View view) {
//        startActivity(new Intent(this, MainActivity.class));
        int startPosition = mAdapter.getItemCount();
        mAdapter.addItem();
        mAdapter.notifyItemRangeInserted(startPosition, 10);
    }
}
