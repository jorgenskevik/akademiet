package com.example.jorgenskevik.e_cardholders;

import android.*;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.system.ErrnoException;
import android.util.DisplayMetrics;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jorgenskevik.e_cardholders.Variables.KVTVariables;
import com.example.jorgenskevik.e_cardholders.models.SessionManager;
import com.example.jorgenskevik.e_cardholders.models.User;
import com.example.jorgenskevik.e_cardholders.remote.UserAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by jorgenskevik on 22.07.2018.
 */


public class Picture_info extends Activity {
    TextView button_back, continue_picture, pick_photo, information_picture; //textview_crop;
    ImageView profil_picture;
    //private CropImageView mCropImageView;
    private Uri mCropImageUri;
    String authToken, fourDigits;
    HashMap<String, String> userDetails, user, unit_member_ship;
    SessionManager sessionManager;
    int user_id;
    Uri imageUri;
    String photo_phat;
    User get_user;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crop_photo);

        //mCropImageView = (CropImageView) findViewById(R.id.crop123);
        button_back = (TextView) findViewById(R.id.back_button);
        continue_picture = (TextView) findViewById(R.id.done_button);
        profil_picture = (ImageView) findViewById(R.id.sircle);
        pick_photo = (TextView) findViewById(R.id.pick_photo);
        information_picture = (TextView) findViewById(R.id.this_is_how);
        //textview_crop = (TextView) findViewById(R.id.textview_crop);

        sessionManager = new SessionManager(getApplicationContext());


            button_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent back = new Intent(Picture_info.this, BarCodeActivity.class);
                startActivity(back);
            }
        });

        pick_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(getPickImageChooserIntent(), 200);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            imageUri = getPickImageResultUri(data);

            // For API >= 23 we need to check specifically that we have permissions to read external storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we get error.
            boolean requirePermissions = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                    isUriRequiresPermissions(imageUri)) {

                // request permissions and handle the result in onRequestPermissionsResult()
                requirePermissions = true;
                mCropImageUri = imageUri;
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }

            if (!requirePermissions) {
                profil_picture.setImageURI(imageUri);
                android.net.Uri imageUri = data.getData();
                String[] filePath = {MediaStore.Images.Media.DATA};
                android.database.Cursor cursor = getContentResolver().query(imageUri, filePath, null, null, null);
                assert cursor != null;
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePath[0]);
                String mediaPath = cursor.getString(columnIndex);
                cursor.close();
                //Picasso.with(this).load(mediaPath).into(profil_picture);

                sessionManager.setMedia_path(mediaPath);

                information_picture.setText(R.string.your_picture);
                continue_picture.setTextColor(ContextCompat.getColor(this, R.color.logobluecolor));
                //textview_crop.setTextColor(ContextCompat.getColor(this, R.color.logobluecolor));
                pick_photo.setTextColor(ContextCompat.getColor(this, R.color.line_color));

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //mCropImageView.setImageUriAsync(mCropImageUri);
            profil_picture.setImageURI(mCropImageUri);
            information_picture.setText(R.string.your_picture);
            continue_picture.setTextColor(ContextCompat.getColor(this, R.color.logobluecolor));
            //textview_crop.setTextColor(ContextCompat.getColor(this, R.color.logobluecolor));
            pick_photo.setTextColor(ContextCompat.getColor(this, R.color.line_color));

        } else {
            Toast.makeText(this, "Required permissions are not granted", Toast.LENGTH_LONG).show();
        }
    }

    /*public void onCropImageClick(View view) {
        Bitmap cropped = mCropImageView.getCroppedImage(500, 500);
        if (cropped != null)
          profil_picture.setImageBitmap(cropped);
    }*/

    public Bitmap rotateBitmap(Bitmap original, float degrees) {
        int width = original.getWidth();
        int height = original.getHeight();

        Matrix matrix = new Matrix();
        matrix.preRotate(degrees);

        Bitmap rotatedBitmap = Bitmap.createBitmap(original, 0, 0, width, height, matrix, true);
        Canvas canvas = new Canvas(rotatedBitmap);
        canvas.drawBitmap(original, 5.0f, 0.0f, null);

        return rotatedBitmap;
    }

    public void onContinue(View view) {

        if (profil_picture.getDrawable() == null){
            Toast.makeText(getApplicationContext(), R.string.set_picture, Toast.LENGTH_LONG).show();
            return;
        }
        sessionManager = new SessionManager(getApplicationContext());
        userDetails = sessionManager.getUserDetails();
        unit_member_ship = sessionManager.getUnitMemberDetails();
        final String student_number = unit_member_ship.get(SessionManager.KEY_STUDENTNUMBER);
        authToken = "token " + userDetails.get(SessionManager.KEY_TOKEN);
        user_id = Integer.parseInt(userDetails.get(SessionManager.KEY_UNIT_ID));
        fourDigits = userDetails.get(SessionManager.KEY_PICTURETOKEN);
        user = sessionManager.getMedia_path();
        photo_phat = user.get(SessionManager.KEY_MEDIA_PATH);

        final File file = new File(photo_phat);

        String mimeType = getMimeType(file);

        System.out.println(fourDigits);
        RequestBody reqFile = RequestBody.create(MediaType.parse(mimeType), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("photo", file.getName(), reqFile);
        RequestBody name = RequestBody.create(MediaType.parse("multipart/form-data"), fourDigits);

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(KVTVariables.getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        UserAPI userapi = retrofit.create(UserAPI.class);


        userapi.postPicture(authToken, body, name).enqueue(new Callback<User>() {

            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()){
                    get_user = response.body();
                    SessionManager sess = new SessionManager(getApplicationContext());

                    sess.updatePicture(get_user.getPicture());
                    sess.updatePath(photo_phat);
                    sess.updateTurn(getCameraPhotoOrientation(photo_phat));
                    sess.updatePictureToken("BRUKT");
                    sess.update_boolean(true);
                    get_user.setHas_set_picture(true);
                    ContextWrapper cw = new ContextWrapper(getApplicationContext());
                    File directory = cw.getDir(student_number, Context.MODE_PRIVATE);
                    File myImageFile = new File(directory, "my_image." + getMimeType(file));

                    Picasso.with(getApplicationContext()).invalidate(myImageFile);

                    //lagre bildet lokalt
                    Picasso.with(getApplicationContext()).load(get_user.getPicture()).into(picassoImageTarget(getApplicationContext(), student_number, "my_image.jpeg"));
                    Intent i = new Intent(Picture_info.this, UserActivity.class);
                    startActivity(i);


                }else{
                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, R.string.updatePicture, duration);
                    toast.show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, R.string.PictureNotUpdated, duration);
                toast.show();
            }

        });
    }

    public Intent getPickImageChooserIntent() {

        // Determine Uri of camera image to save.
        Uri outputFileUri = getCaptureImageOutputUri();

        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getPackageManager();

        // collect all camera intents
        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }

        // collect all gallery intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        // the main intent is the last in the list (fucking android) so pickup the useless one
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, "Select source");

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }

    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "pickImageResult.jpeg"));
        }
        return outputFileUri;
    }

    public String getCameraPhotoOrientation(String imagePath) {
        String rotate = "kortfri";
        try {
            File imageFile = new File(imagePath);
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = "270";
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = "180";
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = "90";
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }

    private Target picassoImageTarget(Context context, final String imageDir, final String imageName) {
        ContextWrapper cw = new ContextWrapper(context);
        final File directory = cw.getDir(imageDir, Context.MODE_PRIVATE); // path to /data/data/yourapp/app_imageDir
        return new Target() {
            @Override
            public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final File myImageFile = new File(directory, imageName); // Create image file
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(myImageFile);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
            }
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                if (placeHolderDrawable != null) {}
            }
        };
    }

    public Uri getPickImageResultUri(Intent data) {
        boolean isCamera = true;
        if (data != null && data.getData() != null) {
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        return isCamera ? getCaptureImageOutputUri() : data.getData();
    }

    public boolean isUriRequiresPermissions(Uri uri) {
        try {
            ContentResolver resolver = getContentResolver();
            InputStream stream = resolver.openInputStream(uri);
            stream.close();
            return false;
        } catch (FileNotFoundException e) {
            if (e.getCause() instanceof ErrnoException) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public String getMimeType(File url) {
        String type = null;
        String test = String.valueOf(url);
        test = test.toLowerCase();
        String extension = MimeTypeMap.getFileExtensionFromUrl(test);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }if(type == null){
            type = "image/*";
        }
        return type;
    }



}
