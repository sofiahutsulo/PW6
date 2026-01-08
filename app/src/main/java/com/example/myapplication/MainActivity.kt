package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private enum class Mode { NONE, CONTROL, VARIANT2 }
    private var mode: Mode = Mode.NONE


    private fun controlShr1Rows(): List<EpRow> = listOf(
        EpRow("Шліфувальний верстат (1-4)", 0.92, 0.9, 0.38, 4, 20.0, 0.15, 1.33),
        EpRow("Свердлильний верстат (5-6)", 0.92, 0.9, 0.38, 2, 14.0, 0.12, 1.0),
        EpRow("Фугувальний верстат (9-12)", 0.92, 0.9, 0.38, 4, 42.0, 0.15, 1.33),
        EpRow("Циркулярна пила (13)",       0.92, 0.9, 0.38, 1, 36.0, 0.30, 1.52),
        EpRow("Прес (16)",                 0.92, 0.9, 0.38, 1, 20.0, 0.50, 0.75),
        EpRow("Полірувальний верстат (24)",0.92, 0.9, 0.38, 1, 40.0, 0.20, 1.0),
        EpRow("Фрезерний верстат (26-27)", 0.92, 0.9, 0.38, 2, 32.0, 0.20, 1.0),
        EpRow("Вентилятор (36)",           0.92, 0.9, 0.38, 1, 20.0, 0.65, 0.75),
    )


    private fun controlBigRows(): List<EpRow> = listOf(
        EpRow("Зварювальний трансформатор", 0.92, 0.9, 0.38, 2, 100.0, 0.20, 3.0),
        EpRow("Сушильна шафа",              0.92, 0.9, 0.38, 2, 120.0, 0.80, 0.0), // tgφ у табл. як “-”, берем 0
    )

    // Варіант 2 (табл. 6.8):
    private fun variant2Shr1Rows(): List<EpRow> {
        val base = controlShr1Rows().toMutableList()

        // iліфувальний (індекс 0)
        base[0] = base[0].copy(pKw = 21.0)

        // wиркулярна пила (індекс 3)
        base[3] = base[3].copy(tg = 1.56)

        // gолірувальний (індекс 5)
        base[5] = base[5].copy(kv = 0.22)

        return base
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvMode = findViewById<TextView>(R.id.tvMode)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        val edKrShr = findViewById<EditText>(R.id.edKrShr)
        val edKqShr = findViewById<EditText>(R.id.edKqShr)
        val edKrShop = findViewById<EditText>(R.id.edKrShop)
        val edKqShop = findViewById<EditText>(R.id.edKqShop)

        val btnFillControl = findViewById<Button>(R.id.btnFillControl)
        val btnFillVariant2 = findViewById<Button>(R.id.btnFillVariant2)
        val btnCalc = findViewById<Button>(R.id.btnCalc)
        val btnClear = findViewById<Button>(R.id.btnClear)


        edKrShr.setText("1.25")
        edKqShr.setText("1.0")
        edKrShop.setText("0.7")
        edKqShop.setText("0.7")

        fun setMode(m: Mode) {
            mode = m
            tvMode.text = when (m) {
                Mode.NONE -> "Режим: —"
                Mode.CONTROL -> "Режим: Контрольний приклад (табл. 6.6–6.7)"
                Mode.VARIANT2 -> "Режим: Варіант 2 (табл. 6.8)"
            }
        }

        setMode(Mode.NONE)

        btnFillControl.setOnClickListener {
            setMode(Mode.CONTROL)
            tvResult.text = "Дані завантажено (контрольний приклад). Натисни «Розрахувати»."
        }

        btnFillVariant2.setOnClickListener {
            setMode(Mode.VARIANT2)
            tvResult.text = "Дані завантажено (варіант 2). Натисни «Розрахувати»."
        }

        btnClear.setOnClickListener {
            setMode(Mode.NONE)
            tvResult.text = "Результат..."
        }

        btnCalc.setOnClickListener {
            if (mode == Mode.NONE) {
                tvResult.text = "Спочатку обери режим: «контрольний» або «варіант 2»."
                return@setOnClickListener
            }

            val krShr = readDouble(edKrShr) ?: 1.25
            val kqShr = readDouble(edKqShr) ?: 1.0
            val krShop = readDouble(edKrShop) ?: 0.7
            val kqShop = readDouble(edKqShop) ?: 0.7

            val shr1Rows = if (mode == Mode.CONTROL) controlShr1Rows() else variant2Shr1Rows()
            val bigRows = controlBigRows()

            val shr1 = calcGroup(shr1Rows, kr = krShr, kq = kqShr)


            val shr2 = shr1
            val shr3 = shr1

            val shop = calcShopTotal(shrs = listOf(shr1, shr2, shr3), bigRows = bigRows, kr = krShop, kq = kqShop)

            tvResult.text = buildString {
                append("=== ПР6: Результати (${if (mode == Mode.CONTROL) "контрольний" else "варіант 2"}) ===\n\n")

                append("1) Рядки ШР1 (з розрахунковими колонками):\n")
                shr1Rows.forEach { r ->
                    val c = calcRow(r)
                    append("- ${r.name}\n")
                    append("  n·Рн=${fmt(c.nP)} кВт;  n·Рн·КВ=${fmt(c.nPKv)} кВт;  n·Рн·КВ·tgφ=${fmt(c.nPKvTg)} квар;  n·Рн^2=${fmt(c.nP2)}\n")
                    append("  Ip(ЕП) = ${fmt(c.ipSingle)} A\n")
                }

                append("\n2) Підсумок ШР1:\n")
                append("Σ(n·Рн) = ${fmt(shr1.sumNP)} кВт\n")
                append("Σ(n·Рн·КВ) = ${fmt(shr1.sumNPKv)} кВт\n")
                append("Σ(n·Рн·КВ·tgφ) = ${fmt(shr1.sumNPKvTg)} квар\n")
                append("Σ(n·Рн^2) = ${fmt(shr1.sumNP2)}\n")
                append("Кв = ${fmt(shr1.kvGroup)}\n")
                append("ne = ${fmt(shr1.ne)}\n")
                append("Kр = ${fmt(krShr)}\n")
                append("Pp = Kр·Σ(n·Рн·КВ) = ${fmt(shr1.pp)} кВт\n")
                append("Qp = Kq·Σ(n·Рн·КВ·tgφ) = ${fmt(shr1.qp)} квар\n")
                append("Sp = √(Pp² + Qp²) = ${fmt(shr1.sp)} кВ·А\n")
                append("Ip (груп.) = Pp / Uн = ${fmt(shr1.ipGroup)} A\n")

                append("\n3) Всього цех (ШР1+ШР2+ШР3 + крупні ЕП):\n")
                append("Σ(n·Рн) = ${fmt(shop.sumNP)} кВт\n")
                append("Σ(n·Рн·КВ) = ${fmt(shop.sumNPKv)} кВт\n")
                append("Σ(n·Рн·КВ·tgφ) = ${fmt(shop.sumNPKvTg)} квар\n")
                append("Σ(n·Рн^2) = ${fmt(shop.sumNP2)}\n")
                append("Кв(цех) = ${fmt(shop.kvGroup)}\n")
                append("ne(цех) = ${fmt(shop.ne)}\n")
                append("Kр(цех) = ${fmt(krShop)}\n")
                append("Pp(цех) = ${fmt(shop.pp)} кВт\n")
                append("Qp(цех) = ${fmt(shop.qp)} квар\n")
                append("Sp(цех) = ${fmt(shop.sp)} кВ·А\n")
                append("Ip(цех) = ${fmt(shop.ipGroup)} A\n")

                if (mode == Mode.CONTROL) {
                    append("\n(Для контролю з табл. 6.7 очікувано близько: \n")
                    append("ШР1: Кв≈0.20, ne≈15, Pp≈118.95, Qp≈107, Sp≈160.2, Ip≈313 A;\n")
                    append("Цех: Кв≈0.32, ne≈56, Pp≈526.4, Qp≈459.9, Sp≈699, Ip≈1385.2 A.)\n")
                }
            }
        }
    }



    private data class EpRow(
        val name: String,
        val eta: Double,
        val cos: Double,
        val uKv: Double,
        val n: Int,
        val pKw: Double,
        val kv: Double,
        val tg: Double
    )

    private data class RowCalc(
        val nP: Double,
        val nPKv: Double,
        val nPKvTg: Double,
        val nP2: Double,
        val ipSingle: Double
    )

    private data class GroupCalc(
        val sumNP: Double,
        val sumNPKv: Double,
        val sumNPKvTg: Double,
        val sumNP2: Double,
        val kvGroup: Double,
        val ne: Double,
        val pp: Double,
        val qp: Double,
        val sp: Double,
        val ipGroup: Double
    )

    private fun calcRow(r: EpRow): RowCalc {
        val nP = r.n * r.pKw
        val nPKv = nP * r.kv
        val nPKvTg = nPKv * r.tg
        val nP2 = r.n * r.pKw * r.pKw

        // Ip для окремого ЕП (п.3.2): I = n*P / (sqrt(3)*U*cosφ*η)
        val ip = if (r.uKv > 0 && r.cos > 0 && r.eta > 0) {
            nP / (sqrt(3.0) * r.uKv * r.cos * r.eta)
        } else 0.0

        return RowCalc(nP, nPKv, nPKvTg, nP2, ip)
    }

    private fun calcGroup(rows: List<EpRow>, kr: Double, kq: Double): GroupCalc {
        val calcs = rows.map { calcRow(it) }

        val sumNP = calcs.sumOf { it.nP }
        val sumNPKv = calcs.sumOf { it.nPKv }
        val sumNPKvTg = calcs.sumOf { it.nPKvTg }
        val sumNP2 = calcs.sumOf { it.nP2 }

        val kvGroup = if (sumNP != 0.0) (sumNPKv / sumNP) else 0.0
        val ne = if (sumNP2 != 0.0) (sumNP * sumNP) / sumNP2 else 0.0

        val pp = kr * sumNPKv
        val qp = kq * sumNPKvTg
        val sp = sqrt(pp * pp + qp * qp)

        // Як у прикладі: Ip групи = Pp / Uн (Uн = 0.38 кВ)
        val u = rows.firstOrNull()?.uKv ?: 0.38
        val ipGroup = if (u != 0.0) pp / u else 0.0

        return GroupCalc(sumNP, sumNPKv, sumNPKvTg, sumNP2, kvGroup, ne, pp, qp, sp, ipGroup)
    }

    private fun calcShopTotal(shrs: List<GroupCalc>, bigRows: List<EpRow>, kr: Double, kq: Double): GroupCalc {
        val bigCalcs = bigRows.map { calcRow(it) }

        val sumNP = shrs.sumOf { it.sumNP } + bigCalcs.sumOf { it.nP }
        val sumNPKv = shrs.sumOf { it.sumNPKv } + bigCalcs.sumOf { it.nPKv }
        val sumNPKvTg = shrs.sumOf { it.sumNPKvTg } + bigCalcs.sumOf { it.nPKvTg }
        val sumNP2 = shrs.sumOf { it.sumNP2 } + bigCalcs.sumOf { it.nP2 }

        val kvGroup = if (sumNP != 0.0) (sumNPKv / sumNP) else 0.0
        val ne = if (sumNP2 != 0.0) (sumNP * sumNP) / sumNP2 else 0.0

        val pp = kr * sumNPKv
        val qp = kq * sumNPKvTg
        val sp = sqrt(pp * pp + qp * qp)

        val u = 0.38
        val ipGroup = pp / u

        return GroupCalc(sumNP, sumNPKv, sumNPKvTg, sumNP2, kvGroup, ne, pp, qp, sp, ipGroup)
    }

    

    private fun readDouble(ed: EditText): Double? {
        val s = ed.text?.toString()?.trim().orEmpty()
        if (s.isEmpty()) return null
        return s.replace(',', '.').toDoubleOrNull()
    }

    private fun fmt(x: Double): String = String.format(Locale.US, "%.4f", x).replace('.', ',')

}
