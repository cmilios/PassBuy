package thedreamteam.passbuy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SearchResults extends PortraitActivity implements PopupQuantityDialog.DialogListener {

    private static final String BUNDLE_NAME = "bundle";
    private static final String BASKET_NAME = "basket";
    private GsonWorker gson = new GsonWorker();
    private GsonFileWorker gsonfw = new GsonFileWorker();
    private List<Product> products = new ArrayList<>();
    private List<StorePrice> prices;
    private Basket basket = new Basket();
    private SearchResultsAdapter mAdapter = new SearchResultsAdapter(this, products, basket);
    private EditText searchText;
    private Thread t = new Thread();
    private int categoryId;
    private Context mContext;

    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_results);

        mContext = this.getBaseContext();

        ImageButton searchButton = findViewById(R.id.search_button);
        ImageButton homeScreen = findViewById(R.id.homeButton);
        searchText = findViewById(R.id.search_text);
        backButton = findViewById(R.id.backButton);

        Bundle bundle = getIntent().getBundleExtra(BUNDLE_NAME);
        basket = (Basket) bundle.getSerializable(BASKET_NAME);
        String searchedText = (String) bundle.getCharSequence("search_text");
        categoryId = bundle.getInt("category_id");

        initRecyclerView();

        if (!searchedText.isEmpty())
            searchText.setText(searchedText);
        else
            searchText.setText("");

        if (categoryId == -1)
            getProductsByName();
        else
            getProductsByCategory();

        searchButton.setOnClickListener(v -> getProductsByName());

        homeScreen.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), HomeScreen.class);

            Bundle bundle1 = new Bundle();
            bundle1.putSerializable(BASKET_NAME, basket);
            intent.putExtra(BUNDLE_NAME, bundle1);

            startActivity(intent);
        });

        searchText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchButton.performClick();
                return true;
            }
            return false;
        });

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CategoriesSearchPage.class);

            Bundle bundle2 = new Bundle();
            bundle2.putSerializable(BASKET_NAME, basket);
            intent.putExtra(BUNDLE_NAME, bundle2);

            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        backButton.performClick();
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view_searched_items);

        recyclerView.setHasFixedSize(true);

        mAdapter.replaceBasket(basket);
        recyclerView.setAdapter(mAdapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        DividerItemDecoration lineDivider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(lineDivider);
    }

    private void getProductsByName() {
        Runnable r = () -> {
            products = gson.getSearchResults(searchText.getText().toString());
            if (products != null) {
                mAdapter.replaceList(products);
                runOnUiThread(mAdapter::notifyDataSetChanged);
            } else {
                runOnUiThread(() -> Toast.makeText(mContext,
                        "Δεν υπάρχει προϊόν με αυτή την ονομασία.",
                        Toast.LENGTH_LONG).show());
                runOnUiThread(this::onBackPressed);
            }
        };

        if (t.isAlive()) {
            t.interrupt();
        }
        t = new Thread(r);
        t.start();

    }

    private void getProductsByCategory() {
        Runnable r = () -> {
            products = gson.getProductsByCategory(categoryId - 1);
            if (products != null) {
                mAdapter.replaceList(products);
                runOnUiThread((mAdapter::notifyDataSetChanged));
            } else {
                runOnUiThread(() -> Toast.makeText(mContext,
                        "Δεν λάβαμε τα προϊόντα της κατηγορίας. Ελέγξτε τη σύνδεσή σας.",
                        Toast.LENGTH_LONG).show());
                runOnUiThread(this::onBackPressed);
            }
        };

        if (t.isAlive()) {
            t.interrupt();
        }
        t = new Thread(r);
        t.start();

    }

    @Override
    public void getQuantity(Product p, int q) {
        if (q != 0) {
            new Thread(() -> {
                prices = gson.getProductPrices(p.getProductId());
                if (prices != null) {
                    p.setPrices(prices);
                    if (basket.getProducts().contains(p)) {
                        basket.getQuantities().set(basket.getProducts().indexOf(p), q);
                        mAdapter.replaceBasket(basket);
                        runOnUiThread((mAdapter::notifyDataSetChanged));
                    } else {
                        basket.getProducts().add(p);
                        basket.getQuantities().add(q);
                        mAdapter.replaceBasket(basket);
                        runOnUiThread((mAdapter::notifyDataSetChanged));
                    }
                    gsonfw.saveToFile(basket, mContext);
                }
            }).start();
        }
    }
}
