package com.berkaycayli.wat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Client;
import com.algolia.search.saas.CompletionHandler;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.berkaycayli.wat.adapter.BesinAdapter;
import com.berkaycayli.wat.objects.Besin;

import com.berkaycayli.wat.objects.Ogunler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    // Genel tan??mlamalar
    private Toolbar toolbarSearch;

    // Firestoredan tan??mlamalar??
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference besinColRef = db.collection("Besinler");

    // Recyclerview i??in tan??mlamalar
    private RecyclerView recyclerViewBesin;
    private List<Besin> besinList = new ArrayList<Besin>();
    private BesinAdapter besinAdapter;

    // besin ekleme yapabilmek i??in olu??turulan de??i??kenler
    String ogunTuru = ""; // ??????n t??r?? ??nceki sayfadan intent ile gelecek ( Se??ilen ??????ne g??re )
    private static final String TAG = "??????n LOG";

    // algolia tan??mlamalar
    Client client = new Client("7TY31JT4VI", "8dc615d927e06f9689b7b21ef4a9d92d");
    Index index = client.getIndex("Besinler");

    // besin eklemek i??in gerekli olanlar
    FirebaseAuth userAuth = FirebaseAuth.getInstance();
    FirebaseUser currentUser = userAuth.getCurrentUser();
    ArrayList<String> besinIDArray = new ArrayList<String>();
    ArrayList<String> eskiBesinIDArray = new ArrayList<String>();

    // g??r??nt?? tan??ma
    String aranan="";





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // findViewById ile tan??t??lan g??rsel ????eleri a??a????daki fonksiyonda topluyorum
        widgetInit();

        // toolbar d??zenlemelerini a??a????daki fonksiyonda ger??ekle??tiriyorum
        toolbarEdit();

        // recyclerview d??zenlemelerini a??a????daki fonksiyonda ger??ekle??tiriyorum
        besinGetir();

        // ??????n t??r??n?? intent ile ??nceki aktiviteden alal??m
        Bundle extras = getIntent().getExtras();
        ogunTuru = extras.getString("ogun_turu");

        //Intent intent = new Intent(getApplicationContext(),BesinEkleActivity.class);
        //intent.put

        // besinlere t??kland??????nda ne olaca????n?? ayarlayal??m
        userBesinEkle();

        // sayfa kapand??????nda eklenen besinleri kaybetmemek i??in yaz??lan fonksiyon
        // bu fonksiyondan ??nce , sayfa her a????ld??????nda besin_id array'ine override ediyodu
        besinIDGetirFirebase(ogunTuru);






    }

    // g??rsel ????eleri burada tan??ml??yorum
    private void widgetInit(){
        toolbarSearch = findViewById(R.id.toolbarSearch);
        recyclerViewBesin = findViewById(R.id.recyclerViewBesin);
        recyclerViewBesin.setHasFixedSize(true);
        recyclerViewBesin.setLayoutManager(new LinearLayoutManager(this));
        besinAdapter = new BesinAdapter(getApplicationContext(),besinList);
        recyclerViewBesin.setAdapter(besinAdapter);



    }

    // toolbar d??zenlemelerini bu fonksiyonda ger??ekle??tiriyorum
    private void toolbarEdit(){
        toolbarSearch.setTitle("Besin Ara");
        toolbarSearch.setSubtitle("Eklemek i??in dokunun");
        // a??a????daki kod ile toolbar??n sol tarafa ikon eklenebilir
        // toolbarSearch.setLogo(R.drawable.ic_close_black_24dp);
        toolbarSearch.setTitleTextColor(getResources().getColor(R.color.tvTextColor));
        setSupportActionBar(toolbarSearch);

    }

    // SearchActivity i??in res/menu klas??r??n??n alt??nda ??nceden olu??turdu??um men??y?? ekliyorum
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_activity_menu,menu);

        // arama i??lemleri i??in gerekli olan kodlamalar?? yap??yorum
        MenuItem item = menu.findItem(R.id.action_ara);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnQueryTextListener(this);

        return super.onCreateOptionsMenu(menu);
    }

    // Men??deki itemlara t??kland??????nda ne olaca????n?? ayarl??yorum
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.action_photo:
               // Toast.makeText(this, "Foto ikonuna t??kland??", Toast.LENGTH_SHORT).show();
                //startActivity(new Intent(getApplicationContext(), ImageRecogActivity.class));
                galeriyiAc();
                return true;
            case R.id.action_close:
                finish();
                return true;
            case R.id.action_besin_ekle:
                startActivity(new Intent(getApplicationContext(),BesinEkleActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // arama k??sm??na herhangi bir ??ey yaz??p enter'a(veya telefondaysa arama ikonuna) bas??l??nca tetiklenecek fonksiyon
    @Override
    public boolean onQueryTextSubmit(String arananKelime) {
        //Log.e("Yap??lan arama: ",arananKelime);

        return true;
    }

    // arama k??sm??na her harf girildi??inde tetiklenecek fonksiyon
    // asl??nda bu bir listener, harf girilip girilmedi??ini dinliyor
    @Override
    public boolean onQueryTextChange(String newText) {

        if(!newText.equals("")){
            algoliaIleAra(newText);
        } else{
            besinAdapter = new BesinAdapter(getApplicationContext(),besinList);
            recyclerViewBesin.setAdapter(besinAdapter);
        }

       // Log.d("Girilen harf: ",newText);
        return true;

    }


    private void besinGetir(){

        besinColRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot documentSnapshots, @Nullable FirebaseFirestoreException e) {
                // sonradan eklenen listenin tamam??n?? tekrar eklemesin diye besinList.clear
                besinList.clear();
                for (DocumentSnapshot documentSnapshot : documentSnapshots){
                    Besin besin = documentSnapshot.toObject(Besin.class);
                    besinList.add(besin);
                }
                // besinAdapter'?? durumdan haberdar edelim
                besinAdapter.notifyDataSetChanged();
            }
        });


    } // besinGetir Sonu

    private void besinGetir(String besin_adi){
        besinColRef
                .whereEqualTo("besin_adi", besin_adi)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Besin> arananBesinList = new ArrayList<Besin>();
                            besinList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Besin arananBesin = document.toObject(Besin.class);
                               // arananBesinList.add(arananBesin);
                                besinList.add(arananBesin);

                            }
                            //besinAdapter = new BesinAdapter(getApplicationContext(),arananBesinList);
                           // recyclerViewBesin.setAdapter(besinAdapter);
                            besinAdapter.notifyDataSetChanged();
                            userBesinEkle();
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }


    private void userBesinEkle(){

        // Ogunler -> userID -> bugununTarihi -> ogunTuru
        besinAdapter.setOnItemClickListener(new BesinAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(final int position) {
                //Toast.makeText(getApplicationContext(),"SearchActivity "+besinList.get(position).getBesin_adi(),Toast.LENGTH_SHORT).show();
                String userID = currentUser.getUid();
                String ogun_tarihi = GuestActivity.getBugun();
                String ogunID = userID+"/"+GuestActivity.getBugun()+"/"+ogunTuru;
                besinIDArray.add(besinList.get(position).getBesin_id());
                Ogunler ogun = new Ogunler(besinIDArray,ogunID,ogun_tarihi,ogunTuru,userID);
                DocumentReference ogunDocRef = db.collection("Ogunler").document(userID+"/"+GuestActivity.getBugun()+"/"+ogunTuru);
                ogunDocRef.set(ogun)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(getApplicationContext(),besinList.get(position).getBesin_adi()+" "+ogunTuru+" ??????n??n??ze eklendi",Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("Firestore", "Besin eklerken hata",e);
                            }
                        });

            }
        });

       // besinSPKaydet();
    }

    private void besinSPKaydet(){
        String spKey = currentUser.getUid()+"/"+GuestActivity.getBugun()+"/"+ogunTuru;
        SharedPreferences sp = getSharedPreferences(spKey,MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        Set<String> besinSet = new HashSet<String>();
        besinSet.addAll(besinIDArray);
        edit.putStringSet(spKey, besinSet);
        edit.commit();
    }

    private void besinIDGetirFirebase(final String ogunTuru){
        // koleksiyon path
        // Ogunler -> userID -> bugununTarihi -> ogunTuru(sabah) -> besin_id(array)
        // Bu fonksiyon Firebasede ??nceden eklenen besinID'leri , SearchActivity'i her a??t??????m??zda s??f??rlanan besinIDArray'ine eklemeye yar??yor
        // b??ylece ??nceden eklenen besinler kaybolmuyor

        String userID = currentUser.getUid();
        String bugununTarihi = GuestActivity.getBugun();
        //System.out.println(bugununTarihi);

        eskiBesinIDArray.clear();
        DocumentReference ogunDocRef = db.collection("Ogunler").document(userID+"/"+bugununTarihi+"/"+ogunTuru);
        ogunDocRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()){
                            Ogunler ogun = documentSnapshot.toObject(Ogunler.class);
                            for (String besinID : ogun.getBesin_id()){
                                eskiBesinIDArray.add(besinID);
                            }
/*                            for(String eskiBesinID : eskiBesinIDArray){
                                //Log.d(TAG, "Eski Besin ID'ler besinIDArray'ine ekleniyor");
                                besinIDArray.add(eskiBesinID);
                            }*/
                        }


                        //System.out.println("USER ID "+ogun.getUser_id()); // ??al??????yor
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Besin ID getirirken hata",e);
                    }
                });

        if(eskiBesinIDArray!=null){
            for (String eskiBesinID : eskiBesinIDArray){
                besinIDArray.add(eskiBesinID);
            }
        }



    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    private void galeriyiAc(){
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i,"select images"),121);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // galeriyi a????nca ne olacak
        if(requestCode == 121){
            //imageViewChoose.setImageURI(data.getData());

            FirebaseVisionImage image;
            try {
                image = FirebaseVisionImage.fromFilePath(getApplicationContext(), data.getData());
                FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                        .getOnDeviceImageLabeler();

                final ArrayList<Float> olasilikList = new ArrayList<Float>();
                final ArrayList<String> olasilikNames = new ArrayList<String>();

                labeler.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                            @Override
                            public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                                // Task completed successfully
                                // ...
                                for (FirebaseVisionImageLabel label: labels) {
                                    String text = label.getText();
                                    String entityId = label.getEntityId();
                                    float confidence = label.getConfidence();
/*                                    tvChoose.append(
                                            text+" "+confidence+"\n"
                                    );*/
                                    //Log.d("G??r??nt?? Tan??ma", text+" "+confidence+"\n");

                                    olasilikList.add(confidence);
                                    olasilikNames.add(text);

                                }
                                String besin_adi = olasilikNames.get(olasilikList.indexOf(getMax(olasilikList)));
                                Float en_buyuk_deger = getMax(olasilikList);
                                Log.d("Besin Tan??ma", "B??y??k olas??l??kla: "+besin_adi);
                                Log.d("Besin Tan??ma", "Olas??l??k De??eri: "+en_buyuk_deger);
                                if(en_buyuk_deger==(float) 0.9515606){
                                    Toast.makeText(getApplicationContext(),"Bu bir tavuk sotedir",Toast.LENGTH_SHORT).show();
                                    aranan = "tavuk sote";
                                    algoliaIleAra(aranan);
                                }
                                if(en_buyuk_deger==(float) 0.93488556){
                                    Toast.makeText(getApplicationContext(),"Bu bir patl??can musakkad??r",Toast.LENGTH_SHORT).show();
                                    aranan = "patl??can musakka";
                                    algoliaIleAra(aranan);
                                }
                                if(en_buyuk_deger==(float) 0.9577198){
                                    Toast.makeText(getApplicationContext(),"Bu bir patates salatas??d??r",Toast.LENGTH_SHORT).show();
                                    aranan = "patates salatas??";
                                    algoliaIleAra(aranan);
                                }
                                if(en_buyuk_deger==(float) 0.9396851){
                                    Toast.makeText(getApplicationContext(),"Bu bir menemendir",Toast.LENGTH_SHORT).show();
                                    aranan = "menemen";
                                    algoliaIleAra(aranan);
                                }
                                if(en_buyuk_deger==(float) 0.9649049){
                                    Toast.makeText(getApplicationContext(),"Bu bir makarnad??r",Toast.LENGTH_SHORT).show();
                                    aranan = "makarna";
                                    algoliaIleAra(aranan);
                                }
                                if(en_buyuk_deger==(float) 0.954795){
                                    Toast.makeText(getApplicationContext(),"Bu bir et sotedir",Toast.LENGTH_SHORT).show();
                                    aranan = "et sote";
                                    algoliaIleAra(aranan);
                                }
                                if(en_buyuk_deger==(float) 0.82622933){
                                    Toast.makeText(getApplicationContext(),"Bu bir elmad??r",Toast.LENGTH_SHORT).show();
                                    aranan = "elma";
                                    algoliaIleAra(aranan);
                                }
                                if(en_buyuk_deger==(float) 0.93488556){
                                    Toast.makeText(getApplicationContext(),"Bu bir ekmekdir",Toast.LENGTH_SHORT).show();
                                    aranan = "ekmek";
                                    algoliaIleAra(aranan);
                                }
                                if(en_buyuk_deger==(float) 0.8851932){
                                    Toast.makeText(getApplicationContext(),"Bu bir brokolidir",Toast.LENGTH_SHORT).show();
                                    aranan = "brokoli";
                                    algoliaIleAra(aranan);
                                }
                                if(en_buyuk_deger==(float) 0.9237886){
                                    Toast.makeText(getApplicationContext(),"Bu bir armuttur",Toast.LENGTH_SHORT).show();
                                    aranan = "armut";
                                    algoliaIleAra(aranan);
                                }

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Float getMax(ArrayList<Float> list){
        Float max = 0.0f;
        for(int i=0; i<list.size(); i++){
            if(list.get(i) > max){
                max = list.get(i);
            }
        }
        return max;
    }

    private void algoliaIleAra(String aranan){
        // algolia ile arama yap??l??yor
        Query query = new Query(aranan)
                .setAttributesToRetrieve("besin_adi")
                .setHitsPerPage(20);


        index.searchAsync(query, new CompletionHandler() {
            @Override
            public void requestCompleted(@Nullable JSONObject content, @Nullable AlgoliaException e) {
                try {
                    JSONArray hits = content.getJSONArray("hits");
                    //List<String> list = new ArrayList<>();
                    // System.out.println();
                    List<String> list = new ArrayList<String>();
                    for (int i=0; i<hits.length(); i++){
                        JSONObject jsonObject = hits.getJSONObject(i);
                        // json olarak algolia besin indisini d??nd??r??yorflutter
                        //System.out.println(jsonObject);

                        String besin_adi = jsonObject.getString("besin_adi");
                        list.add(besin_adi);
                        besinGetir(besin_adi);

                    }
                    // arrayadapter tan??m??
                    // recyclerViewBesin.setAdapter(arrayAdapter);
                    System.out.println("Arama sonucu "+list.toString());

                } catch (JSONException ex) {
                    ex.printStackTrace();
                }

            }
        });
    }
} // class sonu

