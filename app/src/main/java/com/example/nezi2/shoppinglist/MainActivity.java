package com.example.nezi2.shoppinglist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nezi2.shoppinglist.model.ShoppingItem;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.ui.FirebaseListAdapter;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "ShoppingList";
    private static final int PREFERENCES_CODE = 299;
    ListView listView;
    //Random
    private CoordinatorLayout coordinatorLayout;

    //Auth
    //User
    private TextView userName;
    private TextView userEmail;
    private ImageView profilePicture;

    //Nav Login Button
    private View nav_login_btn;

    //Nav Register Button
    private View nav_register_btn;

    //Nav Logout Button
    private View nav_logout_btn;

    /* A dialog that is presented until the Firebase authentication finished. */
    private ProgressDialog mAuthProgressDialog;

    /* Data from the authenticated user */
    private AuthData mAuthData;

    /* Listener for Firebase session changes */
    private Firebase.AuthStateListener mAuthStateListener;

    /* *************************************
    *              PASSWORD               *
    ***************************************/
    private Button mPasswordLoginButton;

    /* *************************************
     *            ANONYMOUSLY              *
     ***************************************/
    private Button mAnonymousLoginButton;

    //Service
    //private Service Service = service.Service.getInstance();

    private Firebase sRef;
    private Firebase listsRef;

    private NavigationView navigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Action bar
        //getActionBar().setHomeButtonEnabled(true); //this means we can click "home"

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id
                .coordinatorLayout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.btnAdd);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if not logged it
                if (mAuthData == null) {
                    Snackbar
                            .make(coordinatorLayout, "You need to be registered to add items.", Snackbar.LENGTH_LONG)
                            .setAction("Register", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    showRegisterDialog();
                                }
                            })
                            .setActionTextColor(Color.GREEN)
                            .show();
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("Add new item");

                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                builder.setView(inflater.inflate(R.layout.dialog_newitem, null));

                builder.setPositiveButton("OK", null);
                builder.setNegativeButton("Cancel", null);

                final AlertDialog dialog = builder.create();
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(final DialogInterface d) {
                        Button btnOK = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        btnOK.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                AlertDialog dlg = (AlertDialog) dialog;
                                String name = ((TextView) dlg.findViewById(R.id.newitem_name)).getText() + "";
                                Double quantity = -1.0;
                                try {
                                    quantity = Double.valueOf(((TextView) dlg.findViewById(R.id.newitem_quantity)).getText() + "");
                                } catch (Exception e) {
                                    quantity = 0.0;
                                }
                                final ShoppingItem newItem = new ShoppingItem(name, quantity);
                                listsRef.push().setValue(newItem);
                                dialog.dismiss();
                            }
                        });
                    }
                });
                dialog.show();
            }
        });

        /////////////
        // Drawer
        /////////////

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //User details

        userEmail = (TextView) navigationView.getHeaderView(0).findViewById(R.id.userEmail);
        userName = (TextView) navigationView.getHeaderView(0).findViewById(R.id.userName);
        profilePicture = (ImageView) navigationView.getHeaderView(0).findViewById(R.id.profilePicture);

        listView = (ListView) findViewById(R.id.list);
        listView.setEmptyView(findViewById(R.id.emptyList));

        /* Create the Firebase ref that is used for all authentication with Firebase */
        sRef = new Firebase(getResources().getString(R.string.firebase_url));

        /* Setup the progress dialog that is displayed later when authenticating with Firebase */
        mAuthProgressDialog = new ProgressDialog(this);
        mAuthProgressDialog.setTitle("Loading");
        mAuthProgressDialog.setMessage("Authenticating with Firebase...");
        mAuthProgressDialog.setCancelable(false);
        mAuthProgressDialog.show();

        mAuthStateListener = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                mAuthProgressDialog.hide();
                setAuthenticatedUser(authData);
            }
        };
        /* Check if the user is authenticated with Firebase already. If this is the case we can set the authenticated
         * user and hide hide any login buttons */
        sRef.addAuthStateListener(mAuthStateListener);
    }

    /////////
    // LIST
    /////////

    private void setShoppingList() {
        final FirebaseListAdapter<ShoppingItem> fireAdapter = new FirebaseListAdapter<ShoppingItem>(this, ShoppingItem.class, R.layout.listviewitemlayout, listsRef) {

            @Override
            protected void populateView(View view, final ShoppingItem shoppingItem, int i) {
                //Product Name line
                TextView tvName = (TextView) view.findViewById(R.id.itemName);
                tvName.setText(shoppingItem.getName());
                //Description
                TextView tvQuantity = (TextView) view.findViewById(R.id.itemQuantity);
                tvQuantity.setText(shoppingItem.getQuantity() + "");

                final int position = i;
//Button delete + delete controller
                ImageButton btnDelete = (ImageButton) view.findViewById(R.id.itemDelete);
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getRef(position).removeValue();
                        Snackbar
                                .make(coordinatorLayout, "Item: " + shoppingItem.getName() + " was removed.", Snackbar.LENGTH_INDEFINITE)
                                .setAction("UNDO", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        listsRef.push().setValue(shoppingItem);
                                    }
                                })
                                .setActionTextColor(Color.RED)
                                .show();
                    }
                });

                //Product Icon
//                ImageView imageView = (ImageView) view.findViewById(R.id.icon);
//                ProductType s = shoppingItem.getProductType();
//                if (s.equals("Milk")) {
//                    imageView.setImageResource(R.drawable.ic_menu_list);
//                } else if (s.equals("Meet")) {
//                    imageView.setImageResource(R.drawable.ic_menu_login);
//                }
            }
        };
        listView.setAdapter(fireAdapter);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // if changing configurations, stop tracking firebase session.
        sRef.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
         /* If a user is currently authenticated, display a logout menu
          * We want to display menu anyways for other options than logout */
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.item_logout).setVisible(this.mAuthData != null);
//        if (this.mAuthData != null) {
//            return true; //show menu
//        } else {
//            return false;// don't show menu
//        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                setPreferences();
                return true;
            case android.R.id.home:
                Toast.makeText(this, "Application icon clicked!",
                        Toast.LENGTH_SHORT).show();
                return true; //return true, means we have handled the event
            case R.id.item_about:
                Toast.makeText(this, "About item clicked!", Toast.LENGTH_SHORT)
                        .show();
                return true;
case R.id.item_delete_all:
Toast.makeText(this, "Delete item clicked!", Toast.LENGTH_SHORT)
        .show();
new AlertDialog.Builder(this)
        .setMessage("Do you really want to delete all items?")
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final boolean[] failedToMakeCopy = {false};
                final ArrayList<ShoppingItem> tempItems = new ArrayList<>();
                listsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snap : dataSnapshot.getChildren()
                                ) {
                            tempItems.add(snap.getValue(ShoppingItem.class));
                        }
                        failedToMakeCopy[0] = false;
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        failedToMakeCopy[0] = true;
                    }
                });
                if (!failedToMakeCopy[0])
                    listsRef.setValue(null);
                else {
                    Snackbar.make(coordinatorLayout, "Items have been permanently deleted.", Snackbar.LENGTH_LONG).show();
                    return;
                }
                Snackbar
                        .make(coordinatorLayout, "All items have been deleted.", Snackbar.LENGTH_INDEFINITE)
                        .setAction("UNDO", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                for (ShoppingItem item : tempItems
                                        ) {
                                    listsRef.push().setValue(item);
                                }
                            }
                        })
                        .setActionTextColor(Color.RED)
                        .show();
            }
        })
        .setNegativeButton(android.R.string.no, null).show();
return true;
            case R.id.item_logout:
                logout();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            showLoginDialog();
            return true;
        } else if (id == R.id.nav_logout) {
            logout();
            return true;
        } else if (id == R.id.nav_register) {
            showRegisterDialog();
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    ////////////////////////////////////////////////////////////////
    //
    //                      REGISTER DIALOG
    //
    ////////////////////////////////////////////////////////////////

    void showRegisterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Enter your email address and password")
                .setTitle("Sign Up");

        LayoutInflater inflater = this.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_register, null));

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mAuthProgressDialog = new ProgressDialog(MainActivity.this);
                mAuthProgressDialog.show();
                AlertDialog dlg = (AlertDialog) dialog;
                final String email = ((TextView) dlg.findViewById(R.id.regdialog_email)).getText().toString();
                final String password = ((TextView) dlg.findViewById(R.id.regdialog_password)).getText().toString();

                sRef.createUser(email, password, new Firebase.ResultHandler() {
                    @Override
                    public void onSuccess() {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mAuthProgressDialog.hide();
                                sRef.authWithPassword(email, password, new Firebase.AuthResultHandler() {
                                    @Override
                                    public void onAuthenticated(AuthData authData) {
                                        setAuthenticatedUser(authData);
                                        Map<String, String> map = new HashMap<String, String>();
                                        map.put("provider", authData.getProvider());
                                        if (authData.getProviderData().containsKey("displayName")) {
                                            String name = authData.getProviderData().get("displayName").toString();
                                            map.put("displayName", name);
                                        }
                                        if (authData.getProviderData().containsKey("email")) {
                                            String email = authData.getProviderData().get("email").toString();
                                            map.put("email", email);
                                        }
                                        if (authData.getProviderData().containsKey("profileImageURL")) {
                                            String profileImageURL = authData.getProviderData().get("profileImageURL").toString();
                                            map.put("profileImageURL", profileImageURL);
                                        }
                                        sRef.child("users").child(authData.getUid()).setValue(map);
                                    }

                                    @Override
                                    public void onAuthenticationError(FirebaseError firebaseError) {
                                        showErrorDialog(firebaseError.toString());
                                    }
                                });
                            }
                        }, 2000);
                    }

                    @Override
                    public void onError(FirebaseError firebaseError) {
                        mAuthProgressDialog.hide();
                        // sRef.authWithPassword(email, password, null);
                        showErrorDialog(firebaseError.toString());
                    }
                });

                mAuthProgressDialog.hide();
                dlg.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    ////////////////////////////////////////////////////////////////
    //
    //                      LOGIN DIALOG
    //
    ////////////////////////////////////////////////////////////////

    void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();

        final View view = inflater.inflate(R.layout.dialog_signin, null);
        builder.setView(view)
                .setIcon(R.drawable.ic_menu_gallery)
                .setTitle("Login")
                .setPositiveButton(R.string.signin, null)
                .setNegativeButton(R.string.cancel, null);


        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                final AlertDialog d = (AlertDialog) dialog;
                if (d != null) {
                    Button positiveButton = (Button) d.getButton(Dialog.BUTTON_POSITIVE);
                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            EditText loginView = (EditText) d.findViewById(R.id.logindialog_username);
                            EditText passView = (EditText) d.findViewById(R.id.logindialog_password);
                            String loginText = "";
                            String passTest = "";
                            if (loginView.getText() != null) {
                                loginText = loginView.getText().toString();
                                if (loginText.length() < 1) {
                                    loginView.setError("Enter login");
                                    return;
                                }
                            }
                            if (passView.getText() != null) {
                                passTest = passView.getText().toString();
                                if (passTest.length() < 1) {
                                    passView.setError("Enter password");
                                    return;
                                }
                            }
                            //login
                            loginWithPassword(loginText, passTest);
                            dialog.dismiss();
                        }
                    });
                }
            }
        });
        dialog.show();
    }


    ////////////////////////////////////////////////////////////////
    //
    //                      SETTINGS DIALOG
    //
    ////////////////////////////////////////////////////////////////

    //This will be called when other activities in our application
    //are finished.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PREFERENCES_CODE) //exited our preference screen
        {
            //get preferences when we get back from settings
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setPreferences() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    //Get preferences whenever we want
    public void getPreferences() {
        SharedPreferences prefs = getSharedPreferences("my_prefs", MODE_PRIVATE);
        String email = prefs.getString("email", "");

    }

    /**
     * Unauthenticate from Firebase and from providers where necessary.
     */
    private void logout() {
        if (this.mAuthData != null) {
            /* logout of Firebase */
            sRef.unauth();
            /* Update authenticated user and show login buttons */
            setAuthenticatedUser(null);
        }
    }

    /**
     * Once a user is logged in, take the mAuthData provided from Firebase and "use" it.
     */
    private void setAuthenticatedUser(AuthData authData) {
        if (authData != null) {

            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.activity_main_drawer_loggedin);

            String name = authData.getUid();

            if (name != null) {
                userName.setText(name);
            }
            if (authData.getProviderData().containsKey("email")) {
                String email = authData.getProviderData().get("email").toString();
                userEmail.setText(email);
            }
            if (authData.getProviderData().containsKey("profileImageURL")) {
                String profileImageURL = authData.getProviderData().get("profileImageURL").toString();
                new DownloadImageTask(profilePicture).execute(profileImageURL);
            }
            // load lists for authenitcated user
            listsRef = new Firebase(getResources().getString(R.string.firebase_lists_url) + "/" + authData.getUid());
            listsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Log.e(TAG, "There are " + snapshot.getChildrenCount() + " items");
                    setShoppingList();
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    Log.e(TAG, "The read failed: " + firebaseError.getMessage());
                }
            });
            // select the first list

            //set listview
            // setShoppingList(listsRef);
        } else {
            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.activity_main_drawer_loggedout);
            userEmail.setText("");
            userName.setText("");
            profilePicture.setImageResource(android.R.drawable.sym_def_app_icon);
            //clear list
            listView.setAdapter(null);
        }
        this.mAuthData = authData;
        /* invalidate options menu to hide/show the logout button */
        supportInvalidateOptionsMenu();
    }

    /**
     * Show errors to users
     */
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /* ************************************
     *              PASSWORD              *
     **************************************
     */
    public void loginWithPassword(String username, String password) {
        mAuthProgressDialog.show();
        sRef.authWithPassword(username, password, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                mAuthProgressDialog.hide();
                Log.i(TAG, "auth successful");
                setAuthenticatedUser(authData);
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                mAuthProgressDialog.hide();
                showErrorDialog(firebaseError.toString());
            }
        });
    }

    /*
       final AsyncTask<Params, Progress, Result>
           execute(Params... params)
               Executes the task with the specified parameters.
    */
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        public DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        /*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */
        protected Bitmap doInBackground(String... urls) {
            String urlOfImage = urls[0];
            Bitmap logo = null;
            try {
                InputStream is = new URL(urlOfImage).openStream();
                /*
                    decodeStream(InputStream is)
                        Decode an input stream into a bitmap.
                 */
                logo = BitmapFactory.decodeStream(is);
            } catch (Exception e) { // Catch the download exception
                e.printStackTrace();
            }
            return logo;
        }

        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }
}
