package com.app2.eyngel2;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.webkit.WebResourceRequest;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private WebView eyngel;
    private WebSettings eyngelSettings;
    private ValueCallback<Uri[]> mUploadMessage;
    private final static int FILE_CHOOSER_RESULT_CODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 4;
    private final static long UPDATE_INTERVAL = 30 * 60 * 1000; // 30 minutos en milisegundos
    private boolean permissionsRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verificar si al menos uno de los permisos ya se ha otorgado
        if (!permissionsGranted()) {
            // Si no se han otorgado, mostrar un diálogo de confirmación al usuario
            showPermissionConfirmationDialog();
        } else {
            // Si al menos un permiso se otorga automáticamente o ya se ha otorgado, cargar la URL principal en el WebView
            initializeWebView();
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri sharedUri = intent.getData();
            if (sharedUri != null) {
                String url = sharedUri.toString();
                // Verificar si la URL compartida coincide con un patrón específico
                if (url.startsWith("https://eyngel.com/")) {
                    // Cargar la URL compartida en el WebView
                    eyngel.loadUrl(url);
                } else {
                    // La URL no coincide con el patrón deseado, puedes manejarla de otra manera
                    // Por ejemplo, abrirla en un navegador externo
                    Intent externalIntent = new Intent(Intent.ACTION_VIEW, sharedUri);
                    startActivity(externalIntent);
                }
            }
        }
    }


    private boolean permissionsGranted() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermissionConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permisos Requeridos");
        builder.setMessage("Esta aplicación necesita permisos para acceder a su galería de imágenes, almacenamiento y notificaciones. ¿Desea otorgar los permisos?");
        builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
                                "android.permission.READ_MEDIA_IMAGES", "android.permission.POST_NOTIFICATIONS", Manifest.permission.READ_MEDIA_IMAGES
                        },
                        PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Permiso denegado, muestra un mensaje al usuario o toma alguna acción adicional
                Toast.makeText(MainActivity.this, "Los permisos son necesarios para la aplicación.", Toast.LENGTH_SHORT).show();
                // Puedes mostrar un diálogo de explicación adicional si es necesario.
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void initializeWebView() {
        eyngel = findViewById(R.id.web1);
        eyngelSettings = eyngel.getSettings();
        eyngelSettings.setJavaScriptEnabled(true);
        eyngelSettings.setDomStorageEnabled(true);
        eyngelSettings.setMediaPlaybackRequiresUserGesture(false);

        // Habilitar la caché
        eyngelSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Deshabilitar la capacidad de hacer zoom
        eyngelSettings.setBuiltInZoomControls(false);
        eyngelSettings.setDisplayZoomControls(false);


        // Configurar WebViewClient para manejar enlaces y Android App Links
        eyngel.setWebViewClient(new MyWebViewClient());

        // Configurar WebChromeClient para manejar la selección de archivos
        eyngel.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                mUploadMessage = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent, "Seleccione un archivo"), FILE_CHOOSER_RESULT_CODE);
                return true;
            }
        });



        // Cargar la URL principal en el WebView
        eyngel.loadUrl("https://eyngel.com/");


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (permissionsGranted()) {
                // Al menos uno de los permisos otorgado, cargar la URL principal en el WebView
                initializeWebView();
            } else {
                // Permiso denegado, muestra un mensaje al usuario o toma alguna acción adicional
                Toast.makeText(this, "Los permisos son necesarios para la aplicación.", Toast.LENGTH_SHORT).show();
                // Puedes mostrar un diálogo de explicación adicional si es necesario.
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (mUploadMessage == null) return;
            Uri[] result = null;
            try {
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        String dataString = data.getDataString();
                        if (dataString != null) {
                            result = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (eyngel.canGoBack()) {
            eyngel.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // WebViewClient personalizado para manejar enlaces y Android App Links
        // WebViewClient personalizado para manejar enlaces y Android App Links
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Verificar si la URL debe abrirse en la aplicación
            if (url.startsWith("https://eyngel.com")) {
                // Cargar la URL en el WebView
                view.loadUrl(url);
            } else {
                // Intent para abrir la URL en una aplicación externa
                Intent externalIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(externalIntent);
            }
            return true;
        }
    }
    }