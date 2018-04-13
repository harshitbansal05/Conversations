package eu.siacs.conversations.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import eu.siacs.conversations.R;
import eu.siacs.conversations.databinding.ActivitySolidColorBinding;
import eu.siacs.conversations.ui.adapter.SolidColorAdapter;

public class SolidColorActivity extends AppCompatActivity implements SolidColorAdapter.OnSolidColorSelected {

    private final int NUM_COLUMNS = 3;
    private RecyclerView solidColorRecyclerView;
    private SolidColorAdapter solidColorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solid_color);
        ActivitySolidColorBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.activity_solid_color, null, false);
        solidColorRecyclerView = binding.solidColorRecyclerView;
        solidColorRecyclerView.setHasFixedSize(true);
        solidColorRecyclerView.setLayoutManager(new GridLayoutManager(this, NUM_COLUMNS));
        solidColorAdapter = new SolidColorAdapter(this, getSolidColorList());
        solidColorRecyclerView.setAdapter(solidColorAdapter);
    }

    private int[] getSolidColorList() {
        String[] colors = getResources().getStringArray(R.array.solid_colors);
        int[] parsedSolidColors = new int[colors.length];
        for (int i = 0; i < colors.length; ++i) {
            parsedSolidColors[i] = Color.parseColor(colors[i]);
        }
        return parsedSolidColors;
    }

    @Override
    public void onSolidColorSelected(int color) {
        Intent intent = new Intent(this, ConversationsActivity.class);
        intent.putExtra("color", color);
        startActivity(intent);
        finish();
    }
}
