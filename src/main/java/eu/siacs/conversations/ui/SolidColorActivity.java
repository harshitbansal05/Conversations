package eu.siacs.conversations.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;

import eu.siacs.conversations.R;
import eu.siacs.conversations.databinding.ActivitySolidColorBinding;
import eu.siacs.conversations.ui.adapter.SolidColorAdapter;
import eu.siacs.conversations.ui.util.SolidColorItemDecoration;

import static eu.siacs.conversations.ui.XmppActivity.configureActionBar;

public class SolidColorActivity extends AppCompatActivity implements SolidColorAdapter.OnSolidColorSelected {

    private final int COLUMN_WIDTH = 120;
    private RecyclerView solidColorRecyclerView;
    private SolidColorAdapter solidColorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySolidColorBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_solid_color);
        setSupportActionBar(binding.toolbar);
        configureActionBar(getSupportActionBar());
        solidColorRecyclerView = binding.solidColorRecyclerView;
        solidColorRecyclerView.setHasFixedSize(true);
        solidColorRecyclerView.setLayoutManager(new GridLayoutManager(this, getSpanCount()));
        solidColorRecyclerView.addItemDecoration(new SolidColorItemDecoration(getSpacing(), getSpanCount()));
        solidColorAdapter = new SolidColorAdapter(this, getSolidColorList());
        solidColorRecyclerView.setAdapter(solidColorAdapter);
    }

    private int getSpacing() {
        int spanCount = getSpanCount();
        return (getResources().getDisplayMetrics().widthPixels - spanCount * dpToPx(COLUMN_WIDTH)) / (spanCount + 1);
    }

    private int getSpanCount() {
        return getResources().getDisplayMetrics().widthPixels / dpToPx(COLUMN_WIDTH);
    }

    private int dpToPx(int i) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, i, getResources().getDisplayMetrics());
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
        setResult(RESULT_OK, intent);
        finish();
    }
}
