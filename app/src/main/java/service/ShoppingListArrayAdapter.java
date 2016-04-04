package service;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nezi2.shoppinglist.R;

import java.util.ArrayList;

import model.ProductType;

import model.ShoppingItem;

public class ShoppingListArrayAdapter<T extends ShoppingItem> extends ArrayAdapter<T> {
    private final Context context;
    private final ArrayList<T> values;

    public ShoppingListArrayAdapter(Context context, int layout, ArrayList<T> values) {
        super(context, layout, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //Row
        View rowView = inflater.inflate(R.layout.listviewitemlayout, parent, false);
        //Product Name line
        TextView textView = (TextView) rowView.findViewById(R.id.firstLine);
        textView.setText(values.get(position).getName());
        //Description
        TextView tvDesc = (TextView) rowView.findViewById(R.id.secondLine);
        tvDesc.setText(values.get(position).getDescription());
        //Price
        TextView tvPrice = (TextView) rowView.findViewById(R.id.price);
        tvPrice.setText(values.get(position).getPrice() + " kr");
        //Product Icon
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        ProductType s = values.get(position).getProductType();
        if (s.equals("Milk")) {
            imageView.setImageResource(R.drawable.ic_menu_list);
        } else if (s.equals("Meet")) {
            imageView.setImageResource(R.drawable.ic_menu_login);
        }

        ///
        //Maybe try catch here???
        ///

        return rowView;
    }
}