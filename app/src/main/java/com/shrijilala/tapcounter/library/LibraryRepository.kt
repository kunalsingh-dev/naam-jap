package com.shrijilala.tapcounter.library

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Pad(
    val no: Int,
    val text: Map<String, String> = emptyMap(),
    val meaning: Map<String, String> = emptyMap()
)

data class TextDoc(
    val id: String,
    val section: String,
    val title: String,
    val hasMeaning: Boolean = false,
    val practice: Boolean = false,
    val totalPads: Int = 0,
    val audioUrl: String = "",
    val pads: List<Pad> = emptyList()
) {
    val maxPadNo: Int get() = maxOf(totalPads, pads.maxOfOrNull { it.no } ?: 0)
}

val LANGUAGES: LinkedHashMap<String, String> = linkedMapOf(
    "hi" to "हिंदी", "en" to "English", "gu" to "ગુજરાતી",
    "bn" to "বাংলা", "mr" to "मराठी", "ta" to "தமிழ்", "te" to "తెలుగు"
)

/**
 * Library of texts. Bundled structure lives in code (defaults()); everything the user
 * types/pastes is saved to filesDir/library_user.json. New default texts added in
 * app updates are merged in automatically without touching the user's edits.
 */
object LibraryRepository {
    var docs by mutableStateOf<List<TextDoc>>(emptyList())
        private set
    private var file: File? = null

    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.filesDir, "library_user.json")
        file = f
        val loaded = runCatching { if (f.exists()) fromJson(f.readText()) else null }.getOrNull()
        val defs = defaults()
        docs = if (loaded == null) defs
        else loaded + defs.filter { d -> loaded.none { it.id == d.id } }
    }

    fun doc(id: String): TextDoc? = docs.firstOrNull { it.id == id }

    fun upsert(docId: String, no: Int, lang: String, text: String?, meaning: String?) {
        docs = docs.map { d ->
            if (d.id != docId) d else {
                val old = d.pads.firstOrNull { it.no == no } ?: Pad(no)
                val newPad = old.copy(
                    text = if (text != null) old.text + (lang to text) else old.text,
                    meaning = if (meaning != null) old.meaning + (lang to meaning) else old.meaning
                )
                d.copy(pads = (d.pads.filter { it.no != no } + newPad).sortedBy { it.no })
            }
        }
        save()
    }

    fun setAudio(docId: String, url: String) {
        docs = docs.map { if (it.id == docId) it.copy(audioUrl = url) else it }
        save()
    }

    fun setTotal(docId: String, total: Int) {
        docs = docs.map { if (it.id == docId) it.copy(totalPads = total) else it }
        save()
    }

    private fun save() {
        val f = file ?: return
        runCatching { f.writeText(toJson(docs)) }
    }

    // ---------- JSON ----------
    private fun mapToJson(m: Map<String, String>) = JSONObject().apply { m.forEach { (k, v) -> put(k, v) } }
    private fun jsonToMap(o: JSONObject?): Map<String, String> {
        if (o == null) return emptyMap()
        val r = linkedMapOf<String, String>()
        o.keys().forEach { r[it] = o.getString(it) }
        return r
    }

    private fun toJson(list: List<TextDoc>): String {
        val arr = JSONArray()
        list.forEach { d ->
            arr.put(JSONObject().apply {
                put("id", d.id); put("section", d.section); put("title", d.title)
                put("hasMeaning", d.hasMeaning); put("practice", d.practice)
                put("total", d.totalPads); put("audio", d.audioUrl)
                put("pads", JSONArray().apply {
                    d.pads.forEach { p ->
                        put(JSONObject().apply {
                            put("no", p.no); put("text", mapToJson(p.text)); put("meaning", mapToJson(p.meaning))
                        })
                    }
                })
            })
        }
        return JSONObject().put("texts", arr).toString()
    }

    private fun fromJson(s: String): List<TextDoc> {
        val arr = JSONObject(s).getJSONArray("texts")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val pa = o.getJSONArray("pads")
            TextDoc(
                id = o.getString("id"), section = o.getString("section"), title = o.getString("title"),
                hasMeaning = o.optBoolean("hasMeaning"), practice = o.optBoolean("practice"),
                totalPads = o.optInt("total"), audioUrl = o.optString("audio"),
                pads = (0 until pa.length()).map { j ->
                    val p = pa.getJSONObject(j)
                    Pad(p.getInt("no"), jsonToMap(p.optJSONObject("text")), jsonToMap(p.optJSONObject("meaning")))
                }
            )
        }
    }

    // ---------- bundled structure ----------
    private const val S_VANI = "श्री हित हरिवंश वाणी"
    private const val S_ASHT = "अष्टयाम सेवा पद्धति"
    private const val S_NITYA = "नित्य पाठ रसोपासना"

    private fun defaults(): List<TextDoc> {
        val chaurashi1 = Pad(
            1, mapOf(
                "hi" to "जोई-जोई प्यारौ करै सोई मोहि भावै,\nभावै मोहि जोई सोई-सोई करैं प्यारे ।\n" +
                    "मोकों तौ भावती ठौर प्यारे के नैंनन में,\nप्यारौ भयौ चाहै मेरे नैंनन के तारे ॥\n" +
                    "मेरे तन मन प्राण हूँ ते प्रीतम प्रिय,\nअपने कोटिक प्राण प्रीतम मोसों हारे ।\n" +
                    "(जै श्री) हित हरिवंश हंस-हंसिनी साँवल-गौर,\nकहौ कौन करै जल-तरंगनि न्यारे ॥"
            )
        )
        return listOf(
            TextDoc("chaurashi", S_VANI, "श्रीहित चौरासी जी", practice = true, totalPads = 84, pads = listOf(chaurashi1)),
            TextDoc("sphut", S_VANI, "श्रीहित स्फुट वाणी"),
            TextDoc("radhasudha", S_VANI, "श्रीराधासुधानिधि स्तोत्रम्", hasMeaning = true, practice = true, totalPads = 270),
            TextDoc("sevak_mool", S_VANI, "श्रीहित सेवक वाणी (मूल पाठ)", practice = true),
            TextDoc("sevak_arth", S_VANI, "श्रीहित सेवक वाणी (अर्थ सहित)", hasMeaning = true, practice = true),
            TextDoc("yamuna_arth", S_VANI, "श्रीयमुनाष्टकम् (अर्थ सहित)", hasMeaning = true),
            TextDoc("patradvay", S_VANI, "हरिवंश महाप्रभु के पत्रद्वय"),

            TextDoc("asht_seva", S_ASHT, "अष्टयाम सेवा (ऑडियो सहित)", hasMeaning = true),
            TextDoc("acharya_vandana", S_ASHT, "आचार्य वन्दना"),
            TextDoc("isht_vandana", S_ASHT, "इष्ट वन्दना"),
            TextDoc("guru_vandana", S_ASHT, "गुरु वन्दना"),
            TextDoc("sevak_vandana", S_ASHT, "श्री सेवक वन्दना"),
            TextDoc("seva_vidhi", S_ASHT, "सेवा की विधि एवं नियम"),
            TextDoc("seva_rules_en", S_ASHT, "Method Of Service And Rules"),

            TextDoc("n_shat_leela", S_NITYA, "वृंदावन शत लीला"),
            TextDoc("n_mangalacharan", S_NITYA, "मंगलाचरण"),
            TextDoc("n_mangal_gaan", S_NITYA, "श्रीहित मंगल गान"),
            TextDoc("n_yamunashtak", S_NITYA, "श्रीयमुनाष्टकम्"),
            TextDoc("n_priyaju", S_NITYA, "श्रीप्रियाजू की नामावली"),
            TextDoc("n_lalaju", S_NITYA, "श्रीलालजू की नामावली"),
            TextDoc("n_rasik", S_NITYA, "रसिक नामावली"),
            TextDoc("n_yugal_dhyan", S_NITYA, "युगल ध्यान"),
            TextDoc("n_bhakt", S_NITYA, "श्रीभक्त - नामावली"),
            TextDoc("n_vrindavan", S_NITYA, "श्रीवृन्दावनधाम नामावली"),
            TextDoc("n_ishtaradhan", S_NITYA, "श्रीहित इष्टाराधन प्रकरण"),
            TextDoc("n_isht_stuti", S_NITYA, "इष्ट स्तुति"),
            TextDoc("n_kirtan", S_NITYA, "कीर्तन एवं जयकारा"),
            TextDoc("n_radha28", S_NITYA, "श्रीराधा के 28 नाम"),
            TextDoc("n_padavali", S_NITYA, "पदावली")
        )
    }
}
