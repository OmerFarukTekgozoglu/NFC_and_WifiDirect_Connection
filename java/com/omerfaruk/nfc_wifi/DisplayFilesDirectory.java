package com.omerfaruk.nfc_wifi;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class DisplayFilesDirectory extends Activity {

    private ListView lvFilesDir;

    private String directory;

    private ArrayList<String> contents;


    /*
        * Bu class da display files directory butonuna ait metotlar yer alıyor. data/data/com.<kullaniciAdi>.<AppAdi>/files
        * altındaki icerikleri gosteriyoruz. Daha Önceden txt dosyası gönderiminde kullanılmıştır. Şu anda bir kullanım amacı yoktur.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_files_directory);

        lvFilesDir = (ListView) findViewById(R.id.lvFilesDir);

        directory = String.valueOf(getApplicationContext().getFilesDir());

        contents = new ArrayList<String>();

        File f = new File(directory);
        File[] directoryContents = f.listFiles();

        for (File file : directoryContents){
            if (file.isDirectory()){
                contents.add(file.getName()+"/");
            }else {
                contents.add(file.getName());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,contents);
        lvFilesDir.setAdapter(adapter);
    }
}
