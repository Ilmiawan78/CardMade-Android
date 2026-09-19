package id.cardmate.app

import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private val fields = linkedMapOf<String, TextInputEditText>()
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let(::recognize) }
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        bmp?.let { recognize(InputImage.fromBitmap(it, 0)) }
    }

    override fun onCreate(state: Bundle?) { super.onCreate(state); showHome() }

    private fun shell(title: String): LinearLayout {
        fields.clear()
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,24,24,40) }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        TextView(this).apply { text=title; textSize=28f; setTextColor(Color.rgb(16,24,40)); setTypeface(typeface,1); setPadding(8,20,8,24) }.also(root::addView)
        return root
    }

    private fun showHome() {
        shell("CardMate")
        TextView(this).apply { text="Pembaca & pembuat kartu nama\nData diproses langsung di perangkat."; textSize=16f; setPadding(8,0,8,30) }.also(root::addView)
        action("Pindai kartu dengan kamera") { takePhoto.launch(null) }
        action("Pilih foto kartu") { pickImage.launch("image/*") }
        action("Buat kartu nama digital") { showEditor(false) }
        TextView(this).apply { text="Tidak perlu akun • OCR offline • QR vCard"; gravity=Gravity.CENTER; setPadding(8,30,8,8) }.also(root::addView)
    }

    private fun showEditor(scanned: Boolean, values: Map<String,String> = emptyMap()) {
        shell(if (scanned) "Periksa hasil pindai" else "Buat kartu nama")
        field("name","Nama lengkap",values["name"])
        field("title","Jabatan",values["title"])
        field("company","Perusahaan",values["company"])
        field("phone","Nomor HP / WhatsApp",values["phone"])
        field("email","Email",values["email"])
        field("website","Website",values["website"])
        field("address","Alamat",values["address"], true)
        action("Simpan ke Kontak") { saveContact() }
        action("Buat & simpan kartu PNG + QR") { exportCard() }
        action("Kembali") { showHome() }
    }

    private fun field(key:String, hint:String, value:String?, multi:Boolean=false) {
        val edit = TextInputEditText(this).apply { setText(value.orEmpty()); if(multi) minLines=2 }
        fields[key]=edit
        TextInputLayout(this).apply { this.hint=hint; setPadding(0,5,0,5); addView(edit) }.also(root::addView)
    }

    private fun action(label:String, work:()->Unit) {
        MaterialButton(this).apply { text=label; textSize=16f; setPadding(8,10,8,10); setOnClickListener { work() } }
            .also { root.addView(it, LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,8,0,8) }) }
    }

    private fun recognize(uri: Uri) = recognize(InputImage.fromFilePath(this, uri))
    private fun recognize(image: InputImage) {
        Toast.makeText(this,"Membaca kartu…",Toast.LENGTH_SHORT).show()
        recognizer.process(image).addOnSuccessListener { showEditor(true, parse(it.text)) }
            .addOnFailureListener { Toast.makeText(this,"Teks tidak terbaca. Coba foto lebih terang.",Toast.LENGTH_LONG).show() }
    }

    private fun parse(raw:String): Map<String,String> {
        val lines=raw.lines().map(String::trim).filter(String::isNotBlank)
        val email=Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",RegexOption.IGNORE_CASE).find(raw)?.value.orEmpty()
        val phone=Regex("(?:\\+?62|0)[0-9 ()-]{7,16}").find(raw)?.value?.trim().orEmpty()
        val web=Regex("(?:https?://)?(?:www\\.)?[a-z0-9.-]+\\.[a-z]{2,}(?:/\\S*)?",RegexOption.IGNORE_CASE).findAll(raw).map{it.value}.firstOrNull{!it.contains("@")}.orEmpty()
        val unused=lines.filterNot { it.contains(email,true)||it.contains(phone)||it.contains(web,true) }
        return mapOf("name" to unused.getOrNull(0).orEmpty(),"title" to unused.getOrNull(1).orEmpty(),"company" to unused.getOrNull(2).orEmpty(),"phone" to phone,"email" to email,"website" to web,"address" to unused.drop(3).joinToString(", "))
    }

    private fun v(key:String)=fields[key]?.text?.toString()?.trim().orEmpty()
    private fun saveContact() {
        val i=Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).apply {
            putExtra(ContactsContract.Intents.Insert.NAME,v("name")); putExtra(ContactsContract.Intents.Insert.COMPANY,v("company"))
            putExtra(ContactsContract.Intents.Insert.JOB_TITLE,v("title")); putExtra(ContactsContract.Intents.Insert.PHONE,v("phone"))
            putExtra(ContactsContract.Intents.Insert.EMAIL,v("email")); putExtra(ContactsContract.Intents.Insert.POSTAL,v("address"))
        }; startActivity(i)
    }

    private fun exportCard() {
        val w=1200; val h=700
        val bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); val c=Canvas(bmp)
        c.drawColor(Color.rgb(14,36,74)); val p=Paint(1).apply { color=Color.WHITE; typeface=Typeface.DEFAULT_BOLD }
        p.textSize=66f; c.drawText(v("name").ifBlank{"Nama Anda"},70f,130f,p)
        p.typeface=Typeface.DEFAULT; p.textSize=34f; p.color=Color.rgb(189,210,255); c.drawText(v("title"),70f,190f,p)
        p.color=Color.WHITE; p.textSize=38f; p.typeface=Typeface.DEFAULT_BOLD; c.drawText(v("company"),70f,270f,p)
        p.typeface=Typeface.DEFAULT; p.textSize=29f
        listOf(v("phone"),v("email"),v("website"),v("address")).filter(String::isNotBlank).take(4).forEachIndexed { i,s -> c.drawText(s,70f,355f+i*55f,p) }
        val qr=qrBitmap(vCard(),270); c.drawBitmap(qr,880f,365f,null)
        val name="CardMate_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.png"
        val values=ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME,name); put(MediaStore.Images.Media.MIME_TYPE,"image/png"); put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/CardMate") }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)?.let { uri -> contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG,100,it) }; Toast.makeText(this,"Tersimpan di Pictures/CardMate",Toast.LENGTH_LONG).show() }
    }

    private fun vCard()="BEGIN:VCARD\nVERSION:3.0\nFN:${v("name")}\nORG:${v("company")}\nTITLE:${v("title")}\nTEL:${v("phone")}\nEMAIL:${v("email")}\nURL:${v("website")}\nADR:;;${v("address")};;;;\nEND:VCARD"
    private fun qrBitmap(text:String,size:Int):Bitmap {
        val matrix=MultiFormatWriter().encode(text,BarcodeFormat.QR_CODE,size,size)
        return Bitmap.createBitmap(size,size,Bitmap.Config.RGB_565).also { b -> for(x in 0 until size) for(y in 0 until size) b.setPixel(x,y,if(matrix[x,y]) Color.BLACK else Color.WHITE) }
    }
}
