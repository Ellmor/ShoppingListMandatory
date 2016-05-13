package com.example.nezi2.shoppinglist;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.ui.FirebaseListAdapter;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.ProductType;
import model.ShoppingItem;
import model.ShoppingList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, LoginDialogFragment.OnLoginDialogListener {

    private static final String TAG = "ShoppingList";
    //Random
    private CoordinatorLayout coordinatorLayout;
    ListView listView;

    //Auth

    //User
    private TextView userName;
    private TextView userEmail;
    private ImageView profilePicture;
    private ArrayList<ShoppingList> shoppingLists;
    private ShoppingList selectedShoppingList;

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

    private Firebase FireBaseRef;
    private Firebase FireBaseListRef;
    private Firebase selectedShoppingListItemsRef;

    private TextView mLoggedInStatusTextView;
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
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("Add new item");

                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                builder.setView(inflater.inflate(R.layout.dialog_newitem, null));

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //check if there is a refrence it items in current list
                        if (selectedShoppingListItemsRef != null) {
                            final ShoppingItem newItem = new ShoppingItem("Test", "Description", 0.0F, new ProductType(0, "ETC"));
                            selectedShoppingListItemsRef.getParent().addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    ArrayList<ShoppingItem> items = (ArrayList<ShoppingItem>) snapshot.getValue(List.class);
                                    items.add(newItem);
                                    selectedShoppingListItemsRef.setValue(items);
                                }
                                @Override
                                public void onCancelled(FirebaseError firebaseError) {
                                }
                            });
                        } else {
                            //ERROR You cannot add to an empty list
                        }
                    }
                });
                builder.setNegativeButton("Cancel", null);

                AlertDialog dialog = builder.create();
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

        /* *************************************
         *               GENERAL               *
         ***************************************/
        mLoggedInStatusTextView = (TextView) findViewById(R.id.login_status);

        listView = (ListView) findViewById(R.id.list);
        listView.setEmptyView(findViewById(R.id.emptyList));
        //listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        //Init Firebase
        //Firebase.setAndroidContext(this);

        /* Create the Firebase ref that is used for all authentication with Firebase */
        FireBaseRef = new Firebase(getResources().getString(R.string.firebase_url));

        //FireBaseRef.keepSynced(true);

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
        FireBaseRef.addAuthStateListener(mAuthStateListener);
    }

    /////////
    // LIST
    /////////

    private void setShoppingList(Firebase listRef) {

        FirebaseListAdapter<ShoppingItem> fireAdapter = new FirebaseListAdapter<ShoppingItem>(this, ShoppingItem.class, R.layout.listviewitemlayout, listRef) {

            @Override
            protected void populateView(View view, ShoppingItem shoppingItem, int i) {
                //Product Name line
                TextView textView = (TextView) view.findViewById(R.id.firstLine);
                textView.setText(shoppingItem.getName());
                //Description
                TextView tvDesc = (TextView) view.findViewById(R.id.secondLine);
                tvDesc.setText(shoppingItem.getDescription());
                //Price
                TextView tvPrice = (TextView) view.findViewById(R.id.price);
                tvPrice.setText(shoppingItem.getPrice() + " kr");
                //Product Icon
                ImageView imageView = (ImageView) view.findViewById(R.id.icon);
                ProductType s = shoppingItem.getProductType();
                if (s.equals("Milk")) {
                    imageView.setImageResource(R.drawable.ic_menu_list);
                } else if (s.equals("Meet")) {
                    imageView.setImageResource(R.drawable.ic_menu_login);
                }
            }
        };
        listView.setAdapter(fireAdapter);
    }


    public void emptyShoppingList() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // if changing configurations, stop tracking firebase session.
        FireBaseRef.removeAuthStateListener(mAuthStateListener);
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
                Toast.makeText(this, "Settings icon clicked!",
                        Toast.LENGTH_SHORT).show();
                return true;
            case android.R.id.home:
                Toast.makeText(this, "Application icon clicked!",
                        Toast.LENGTH_SHORT).show();
                return true; //return true, means we have handled the event
            case R.id.item_about:
                Toast.makeText(this, "About item clicked!", Toast.LENGTH_SHORT)
                        .show();
                return true;
            case R.id.item_delete:
                Toast.makeText(this, "Delete item clicked!", Toast.LENGTH_SHORT)
                        .show();
                if (selectedShoppingList.getShoppingItems().size() > 0) {
                    new AlertDialog.Builder(this)
                            .setMessage("Do you really want to delete all items?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //Toast.makeText(MainActivity.this, "Yaay", Toast.LENGTH_SHORT).show();
                                    Snackbar snackbar = Snackbar
                                            .make(coordinatorLayout, "Welcome to AndroidHive", Snackbar.LENGTH_LONG);
                                    snackbar.show();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null).show();
                } else {
                    //If there are not items to be removed
                    //Do nothing
                }
                return true;
            case R.id.item_refresh:
                Toast.makeText(this, "Refresh item clicked!", Toast.LENGTH_SHORT)
                        .show();
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
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

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

                FireBaseRef.createUser(email, password, new Firebase.ResultHandler() {
                    @Override
                    public void onSuccess() {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mAuthProgressDialog.hide();
                        FireBaseRef.authWithPassword(email, password, new Firebase.AuthResultHandler() {
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
                                FireBaseRef.child("users").child(authData.getUid()).setValue(map);
                            }

                            @Override
                            public void onAuthenticationError(FirebaseError firebaseError) {
                                showErrorDialog(firebaseError.toString());
                            }
                        });
                    }

                    @Override
                    public void onError(FirebaseError firebaseError) {
                        mAuthProgressDialog.hide();
                        // FireBaseRef.authWithPassword(email, password, null);
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
        DialogFragment newFragment = LoginDialogFragment.newInstance();
        newFragment.show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public boolean onIsLoginModelValid(DialogFragment dialog) {
        EditText pass = ((LoginDialogFragment) dialog).getPassView();
        EditText login = ((LoginDialogFragment) dialog).getLoginView();
        String strPass = pass.getText().toString();
        String strLogin = login.getText().toString();
        boolean loginValid = false;
        boolean passValid = false;
        if (strLogin.length() >= 1) {
            loginValid = true;
        } else {
            login.setError("Login must be at least 1 character long.");
        }
        if (strPass != null && strPass.length() >= 1) {
            passValid = true;
        } else {
            pass.setError("Password cannot be empty.");
        }
        if (loginValid && passValid) {
            loginWithPassword(strLogin, strPass);
            return true;
        }
        return false;
    }

    //This will be called when other activities in our application
    //are finished.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) //exited our preference screen
        {
            Toast toast =
                    Toast.makeText(getApplicationContext(), "back from preferences", Toast.LENGTH_LONG);
            toast.setText("back from our preferences");
            toast.show();
            //here you could put code to do something.......
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setPreferences(View v) {
        //Here we create a new activity and we instruct the
        //Android system to start it
        Intent intent = new Intent(this, SettingsActivity.class);
        //startActivity(intent); //this we can use if we DONT CARE ABOUT RESULT

        //we can use this, if we need to know when the user exists our preference screens
        startActivityForResult(intent, 1);
    }

    public void getPreferences(View v) {

        //We read the shared preferences from the
        SharedPreferences prefs = getSharedPreferences("my_prefs", MODE_PRIVATE);
        String email = prefs.getString("email", "");
        String gender = prefs.getString("gender", "");
        boolean soundEnabled = prefs.getBoolean("sound", false);

        Toast.makeText(
                this,
                "Email: " + email + "\nGender: " + gender + "\nSound Enabled: "
                        + soundEnabled, Toast.LENGTH_SHORT).show();
    }

    /**
     * Unauthenticate from Firebase and from providers where necessary.
     */
    private void logout() {
        if (this.mAuthData != null) {
            /* logout of Firebase */
            FireBaseRef.unauth();
            /* Logout of any of the Frameworks. This step is optional, but ensures the user is not logged into
             * Facebook/Google+ after logging out of Firebase. */
//            if (this.mAuthData.getProvider().equals("facebook")) {
//                /* Logout from Facebook */
//                LoginManager.getInstance().logOut();
//            } else if (this.mAuthData.getProvider().equals("google")) {
//                /* Logout from Google+ */
//                if (mGoogleApiClient.isConnected()) {
//                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
//                    mGoogleApiClient.disconnect();
//                }
//            }
            /* Update authenticated user and show login buttons */
            setAuthenticatedUser(null);
        }
    }

    /**
     * This method will attempt to authenticate a user to firebase given an oauth_token (and other
     * necessary parameters depending on the provider)
     */
    private void authWithFirebase(final String provider, Map<String, String> options) {
        if (options.containsKey("error")) {
            showErrorDialog(options.get("error"));
        } else {
            mAuthProgressDialog.show();
            if (provider.equals("twitter")) {
                // if the provider is twitter, we pust pass in additional options, so use the options endpoint
                FireBaseRef.authWithOAuthToken(provider, options, new AuthResultHandler(provider));
            } else {
                // if the provider is not twitter, we just need to pass in the oauth_token
                FireBaseRef.authWithOAuthToken(provider, options.get("oauth_token"), new AuthResultHandler(provider));
            }
        }
    }

    /**
     * Once a user is logged in, take the mAuthData provided from Firebase and "use" it.
     */
    private void setAuthenticatedUser(AuthData authData) {
        if (authData != null) {
            /* Hide all the login buttons */
            //mFacebookLoginButton.setVisibility(View.GONE);
            //mGoogleLoginButton.setVisibility(View.GONE);
            //mTwitterLoginButton.setVisibility(View.GONE);
            //mPasswordLoginButton.setVisibility(View.GONE);
            //mAnonymousLoginButton.setVisibility(View.GONE);
            //navigationView.getMenu().setGroupVisible(R.id.loginGroup, false);
            //navigationView.getMenu().setGroupVisible(R.id.logoutGroup, true);
            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.activity_main_drawer_loggedin);

            /* show a provider specific status text */
            mLoggedInStatusTextView.setVisibility(View.VISIBLE);
            String name = null;
            if (authData.getProvider().equals("facebook")
                    || authData.getProvider().equals("google")
                    || authData.getProvider().equals("twitter")) {
                name = (String) authData.getProviderData().get("displayName");
            } else if (authData.getProvider().equals("anonymous")
                    || authData.getProvider().equals("password")) {
                name = authData.getUid();
            } else {
                Log.e(TAG, "Invalid provider: " + authData.getProvider());
            }
            if (name != null) {
                mLoggedInStatusTextView.setText("Logged in as " + name + " (" + authData.getProvider() + ")");
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
            final Firebase listsRef = new Firebase(getResources().getString(R.string.firebase_lists_url) + "/" + authData.getUid());
            listsRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Log.e(TAG, "There are " + snapshot.getChildrenCount() + " lists");
                    //if there are no lists add the default list
                    //maybe should be changed to a way of handling zero lists
                    if (snapshot.getChildrenCount() == 0) {
                        ArrayList<ShoppingItem> si = new ArrayList<ShoppingItem>();
                        si.add(new ShoppingItem("Test", "Description", 0.0F, new ProductType(0, "ETC")));
                        ShoppingList newList = new ShoppingList("Somelist", si);
                        listsRef.push().setValue(newList);
                    } else { //if there are lists
                        for (DataSnapshot listSnapshot : snapshot.getChildren()) {
                            ShoppingList list = listSnapshot.getValue(ShoppingList.class);
                            selectedShoppingListItemsRef = listSnapshot.getRef().child("shoppingItems");
                            setShoppingList(selectedShoppingListItemsRef.getRef());
                            Log.e(TAG, list.toString());
                        }
                    }
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
            /* No authenticated user show all the login buttons */
//            mFacebookLoginButton.setVisibility(View.VISIBLE);
//            mGoogleLoginButton.setVisibility(View.VISIBLE);
//            mTwitterLoginButton.setVisibility(View.VISIBLE);
//            mPasswordLoginButton.setVisibility(View.VISIBLE);
//            mAnonymousLoginButton.setVisibility(View.VISIBLE);
            mLoggedInStatusTextView.setVisibility(View.GONE);
            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.activity_main_drawer_loggedout);
            userEmail.setText("");
            userName.setText("");
            profilePicture.setImageResource(android.R.drawable.sym_def_app_icon);
            emptyShoppingList();
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

    /**
     * Utility class for authentication results
     */
    private class AuthResultHandler implements Firebase.AuthResultHandler {

        private final String provider;

        public AuthResultHandler(String provider) {
            this.provider = provider;
        }

        @Override
        public void onAuthenticated(AuthData authData) {
            mAuthProgressDialog.hide();
            Log.i(TAG, provider + " auth successful");
            setAuthenticatedUser(authData);
            // Authentication just completed successfully :)

        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            mAuthProgressDialog.hide();
            showErrorDialog(firebaseError.toString());
        }
    }

    /* ************************************
     *              PASSWORD              *
     **************************************
     */
    public void loginWithPassword(String username, String password) {
        mAuthProgressDialog.show();
        //FireBaseRef.authWithPassword("susu@o2.pl", "nezi12", new AuthResultHandler("password"));
        FireBaseRef.authWithPassword(username, password, new AuthResultHandler("password"));
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
